/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PixelDrawing.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferByte;
import javax.swing.JPanel;


public class PixelDrawing
{
	static class PolySeg
	{
		int fx,fy, tx,ty, direction, increment;
		PolySeg nextedge;
		PolySeg nextactive;
	}

	/** the EditWindow being drawn */						private EditWindow world;
	/** the size of the EditWindow */						private Dimension sz;
    /** the offscreen opaque image of the window */			private Image img;
	/** opaque layer of the window */						private int [] opaqueData;
	/** size of the opaque layer of the window */			private int total;

	/** the offscreen maps for transparent layers */		private byte [][][] layerBitMaps;
	/** the number of transparent layers */					private int numLayerBitMaps;
	/** the number of bytes per row in offscreen maps */	private int numBytesPerRow;
	/** the nuber of offscreen transparent maps made */		private int numLayerBitMapsCreated;
	/** the color map of the window */						private Color [] colorMap;

	public PixelDrawing(Dimension sz, EditWindow world)
	{
		this.sz = sz;
		this.world = world;

		// allocate pointer to the opaque image
		img = new BufferedImage(sz.width, sz.height, BufferedImage.TYPE_INT_RGB);
		WritableRaster raster = ((BufferedImage)img).getRaster();
		DataBufferInt dbi = (DataBufferInt)raster.getDataBuffer();
		opaqueData = dbi.getData();
		total = sz.height * sz.width;

		// allocate pointers to the overlappable layers
		Technology tech = Technology.getCurrent();
		numLayerBitMaps = tech.getNumTransparentLayers();
		layerBitMaps = new byte[numLayerBitMaps][][];
		for(int i=0; i<numLayerBitMaps; i++) layerBitMaps[i] = null;
		numBytesPerRow = (sz.width + 7) / 8;
		numLayerBitMapsCreated = 0;
		colorMap = tech.getColorMap();
	}

	public Image getImage() { return img; }

	public void clearImage()
	{
		// erase the overlappable bitmaps
		for(int i=0; i<numLayerBitMaps; i++)
		{
			byte [][] layerBitMap = layerBitMaps[i];
			if (layerBitMap == null) continue;
			for(int y=0; y<sz.height; y++)
			{
				byte [] row = layerBitMap[y];
				for(int x=0; x<numBytesPerRow; x++)
					row[x] = 0;
			}
		}

		// erase opaque image
		for(int i=0; i<total; i++) opaqueData[i] = -1;
	}

	public void composite()
	{
		// merge in the transparent layers
		if (numLayerBitMapsCreated > 0)
		{
			byte [][] rows = new byte[numLayerBitMaps][];
			for(int y=0; y<sz.height; y++)
			{
				for(int i=0; i<numLayerBitMaps; i++)
				{
					byte [][] layerBitMap = layerBitMaps[i];
					if (layerBitMap == null) rows[i] = null; else
					{
						rows[i] = layerBitMap[y];
					}
				}
				for(int x=0; x<sz.width; x++)
				{
					int bits = 0;
					for(int i=0; i<numLayerBitMaps; i++)
					{
						if (rows[i] == null) continue;
						int byt = rows[i][x>>3];
						if ((byt & (1 << (x&7))) != 0) bits |= (1<<i);
					}
					int index = y*sz.width + x;
					if (opaqueData[index] == -1)
					{
						if (bits != 0)
						{
							// set a transparent color
							opaqueData[index] = colorMap[bits].getRGB();
						} else
						{
							opaqueData[index] = Color.LIGHT_GRAY.getRGB();
						}
					}
				}
			}
		} else
		{
			// nothing in transparent layers: make sure background color is right
			for(int i=0; i<total; i++)
				if (opaqueData[i] == -1) opaqueData[i] = Color.LIGHT_GRAY.getRGB();
		}
	}

	/**
	 * @return true if this poly was properly rendered
	 */
	public void renderPoly(Poly poly, EGraphics graphics)
	{
		int layerNum = -1;
		if (graphics != null) layerNum = graphics.getTransparentLayer() - 1;
		if (layerNum >= numLayerBitMaps) return;

		Point2D offset = world.getOffset();
		double offx = offset.getX();
		double offy = offset.getY();
		double scale = world.getScale();

		byte [][] layerBitMap = null;
		if (layerNum >= 0)
		{
			if (layerBitMaps[layerNum] == null)
			{
				 // allocate this bitplane dynamically
				layerBitMaps[layerNum] = new byte[sz.height][];
				for(int y=0; y<sz.height; y++)
				{
					byte [] row = new byte[numBytesPerRow];
					for(int x=0; x<numBytesPerRow; x++) row[x] = 0;
					layerBitMaps[layerNum][y] = row;
				}
				numLayerBitMapsCreated++;
			}
			layerBitMap = layerBitMaps[layerNum];
		}

		// now draw it
		Poly.Type style = poly.getStyle();
		Point2D [] points = poly.getPoints();
		if (style == Poly.Type.FILLED)
		{
			Rectangle2D bounds = poly.getBox();
			if (bounds != null)
			{
				// convert coordinates and test for complete clipping
				int lX = world.databaseToScreenX(bounds.getMinX());
				int hX = world.databaseToScreenX(bounds.getMaxX());
				if (lX >= sz.width) return;
				if (hX < 0) return;
				int hY = world.databaseToScreenY(bounds.getMinY());
				int lY = world.databaseToScreenY(bounds.getMaxY());
				if (lY >= sz.height) return;
				if (hY < 0) return;

				// finish clipping
				if (lX < 0) lX = 0;
				if (hX >= sz.width) hX = sz.width-1;
				if (lY < 0) lY = 0;
				if (hY >= sz.height) hY = sz.height-1;

				// draw the box
				drawBox(lX, hX, lY, hY, layerBitMap, graphics);
				return;
			}
			Point [] intPoints = new Point[points.length];
			for(int i=0; i<points.length; i++)
				intPoints[i] = world.databaseToScreen(points[i]);
			Point [] clippedPoints = clipPoly(intPoints, 0, sz.width, 0, sz.height);
			drawPolygon(clippedPoints, layerBitMap, graphics);
			return;
		}
		if (style == Poly.Type.CROSSED)
		{
			Point pt0a = world.databaseToScreen(points[0]);
			Point pt1a = world.databaseToScreen(points[1]);
			Point pt2a = world.databaseToScreen(points[2]);
			Point pt3a = world.databaseToScreen(points[3]);
			Point pt0b = new Point(pt0a);   Point pt0c = new Point(pt0a);
			Point pt1b = new Point(pt1a);   Point pt1c = new Point(pt1a);
			Point pt2b = new Point(pt2a);   Point pt2c = new Point(pt2a);
			Point pt3b = new Point(pt3a);   Point pt3c = new Point(pt3a);
			drawLine(pt0a, pt1a, layerBitMap, graphics, 0);
			drawLine(pt1b, pt2a, layerBitMap, graphics, 0);
			drawLine(pt2b, pt3a, layerBitMap, graphics, 0);
			drawLine(pt3b, pt0b, layerBitMap, graphics, 0);
			drawLine(pt0c, pt2c, layerBitMap, graphics, 0);
			drawLine(pt1c, pt3c, layerBitMap, graphics, 0);
			return;
		}
		if (style == Poly.Type.TEXTCENT || style == Poly.Type.TEXTTOP || style == Poly.Type.TEXTBOT ||
			style == Poly.Type.TEXTLEFT || style == Poly.Type.TEXTRIGHT || style == Poly.Type.TEXTTOPLEFT ||
			style == Poly.Type.TEXTBOTLEFT || style == Poly.Type.TEXTTOPRIGHT || style == Poly.Type.TEXTBOTRIGHT ||
			style == Poly.Type.TEXTBOX)
		{
			Rectangle2D bounds = poly.getBounds2D();
			Rectangle rect = world.databaseToScreen(bounds);
			TextDescriptor descript = poly.getTextDescriptor();
			String str = poly.getString();
			drawText(rect, style, descript, str, layerBitMap, graphics);
			return;
		}
		if (style == Poly.Type.CLOSED || style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 ||
			style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3 || style == Poly.Type.OPENEDO1)
		{
			int lineType = 0;
			if (style == Poly.Type.OPENEDT1) lineType = 1; else
			if (style == Poly.Type.OPENEDT2) lineType = 2; else
			if (style == Poly.Type.OPENEDT3) lineType = 3;

			Point pt1 = world.databaseToScreen(points[0]);
			for(int j=1; j<points.length; j++)
			{
				Point pt2 = world.databaseToScreen(points[j]);
				drawLine(pt1, pt2, layerBitMap, graphics, lineType);
				pt1 = pt2;
			}
			if (style == Poly.Type.CLOSED)
			{
				Point pt2 = world.databaseToScreen(points[0]);
				drawLine(pt1, pt2, layerBitMap, graphics, lineType);
			}
			return;
		}
		if (style == Poly.Type.VECTORS)
		{
			for(int j=0; j<points.length; j+=2)
			{
				Point pt1 = world.databaseToScreen(points[j]);
				Point pt2 = world.databaseToScreen(points[j+1]);
				drawLine(pt1, pt2, layerBitMap, graphics, 0);
			}
			return;
		}
		if (style == Poly.Type.CIRCLE)
		{
			Point center = world.databaseToScreen(points[0]);
			Point edge = world.databaseToScreen(points[1]);
			drawCircle(center, edge, layerBitMap, graphics);
			return;
		}
		if (style == Poly.Type.THICKCIRCLE)
		{
			Point center = world.databaseToScreen(points[0]);
			Point edge = world.databaseToScreen(points[1]);
			drawThickCircle(center, edge, layerBitMap, graphics);
			return;
		} else if (style == Poly.Type.DISC)
		{
			Point center = world.databaseToScreen(points[0]);
			Point edge = world.databaseToScreen(points[1]);
			drawDisc(center, edge, layerBitMap, graphics);
			return;
		}
		if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
		{
			Point center = world.databaseToScreen(points[0]);
			Point edge1 = world.databaseToScreen(points[1]);
			Point edge2 = world.databaseToScreen(points[2]);
			drawCircleArc(center, edge1, edge2, style == Poly.Type.THICKCIRCLEARC, layerBitMap, graphics);
			return;
		}
		if (style == Poly.Type.CROSS)
		{
			// draw the cross
			drawCross(poly, graphics);
//			Point center = world.databaseToScreen(points[0]);
//			int size = 3;
//			if (style == Poly.Type.BIGCROSS) size = 5;
//			drawLine(new Point(center.x-size, center.y), new Point(center.x+size, center.y), layerBitMap, graphics, 0);
//			drawLine(new Point(center.x, center.y-size), new Point(center.x, center.y+size), layerBitMap, graphics, 0);
			return;
		}
		if (style == Poly.Type.BIGCROSS)
		{
			// draw the big cross
			Point center = world.databaseToScreen(points[0]);
			int size = 5;
			drawLine(new Point(center.x-size, center.y), new Point(center.x+size, center.y), layerBitMap, graphics, 0);
			drawLine(new Point(center.x, center.y-size), new Point(center.x, center.y+size), layerBitMap, graphics, 0);
			return;
		}
	}

	/****************************** LINE DRAWING ******************************/

	/*
	 * Method to draw a line on the off-screen buffer.
	 */
	public void drawLine(Point pt1, Point pt2, byte [][] layerBitMap, EGraphics desc, int texture)
	{
		// first clip the line
		if (clipLine(pt1, pt2, 0, sz.width-1, 0, sz.height-1)) return;

		// now draw with the proper line type
		switch (texture)
		{
			case 0: drawSolidLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, desc);       break;
			case 1: drawPatLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, desc, 0x88);   break;
			case 2: drawPatLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, desc, 0xE7);   break;
			case 3: drawThickLine(pt1.x, pt1.y, pt2.x, pt2.y, layerBitMap, desc);       break;
		}
	}

	public void drawCross(Poly poly, EGraphics graphics)
	{
		Point2D [] points = poly.getPoints();
		Point center = world.databaseToScreen(points[0]);
		int size = 3;
		drawLine(new Point(center.x-size, center.y), new Point(center.x+size, center.y), null, graphics, 0);
		drawLine(new Point(center.x, center.y-size), new Point(center.x, center.y+size), null, graphics, 0);
	}

	private void drawSolidLine(int x1, int y1, int x2, int y2, byte [][] layerBitMap, EGraphics desc)
	{
		// get color and pattern information
		Color col = Color.BLACK;
		int [] pattern = null;
		if (desc != null)
		{
			col = desc.getColor();
			if (desc.isPatternedOnDisplay())
				pattern = desc.getPattern();
		}

		// initialize the Bresenham algorithm
		int dx = Math.abs(x2-x1);
		int dy = Math.abs(y2-y1);
		if (dx > dy)
		{
			// initialize for lines that increment along X
			int incr1 = 2 * dy;
			int incr2 = 2 * (dy - dx);
			int d = incr2;
			int x, y, xend, yend, yincr;
			if (x1 > x2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (yend < y) yincr = -1; else yincr = 1;
			if (layerBitMap == null) opaqueData[y * sz.width + x] = col.getRGB(); else
				layerBitMap[y][x>>3] |= (1 << (x&7));

			// draw line that increments along X
			while (x < xend)
			{
				x++;
				if (d < 0) d += incr1; else
				{
					y += yincr;   d += incr2;
				}
				if (layerBitMap == null) opaqueData[y * sz.width + x] = col.getRGB(); else
					layerBitMap[y][x>>3] |= (1 << (x&7));
			}
		} else
		{
			// initialize for lines that increment along Y
			int incr1 = 2 * dx;
			int incr2 = 2 * (dx - dy);
			int d = incr2;
			int x, y, xend, yend, xincr;
			if (y1 > y2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (xend < x) xincr = -1; else xincr = 1;
			if (layerBitMap == null) opaqueData[y * sz.width + x] = col.getRGB(); else
				layerBitMap[y][x>>3] |= (1 << (x&7));

			// draw line that increments along X
			while (y < yend)
			{
				y++;
				if (d < 0) d += incr1; else
				{
					x += xincr;   d += incr2;
				}
				if (layerBitMap == null) opaqueData[y * sz.width + x] = col.getRGB(); else
					layerBitMap[y][x>>3] |= (1 << (x&7));
			}
		}
	}

	private void drawPatLine(int x1, int y1, int x2, int y2, byte [][] layerBitMap, EGraphics desc, int pattern)
	{
		// get color and pattern information
		Color col = Color.BLACK;
		if (desc != null) col = desc.getColor();

		// initialize counter for line style
		int i = 0;

		// initialize the Bresenham algorithm
		int dx = Math.abs(x2-x1);
		int dy = Math.abs(y2-y1);
		if (dx > dy)
		{
			// initialize for lines that increment along X
			int incr1 = 2 * dy;
			int incr2 = 2 * (dy - dx);
			int d = incr2;
			int x, y, xend, yend, yincr;
			if (x1 > x2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (yend < y) yincr = -1; else yincr = 1;
			if (layerBitMap == null) opaqueData[y * sz.width + x] = col.getRGB(); else
				layerBitMap[y][x>>3] |= (1 << (x&7));

			// draw line that increments along X
			while (x < xend)
			{
				x++;
				if (d < 0) d += incr1; else
				{
					y += yincr;   d += incr2;
				}
				if (i == 7) i = 0; else i++;
				if ((pattern & (1 << i)) == 0) continue;
				if (layerBitMap == null) opaqueData[y * sz.width + x] = col.getRGB(); else
					layerBitMap[y][x>>3] |= (1 << (x&7));
			}
		} else
		{
			// initialize for lines that increment along Y
			int incr1 = 2 * dx;
			int incr2 = 2 * (dx - dy);
			int d = incr2;
			int x, y, xend, yend, xincr;
			if (y1 > y2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (xend < x) xincr = -1; else xincr = 1;
			if (layerBitMap == null) opaqueData[y * sz.width + x] = col.getRGB(); else
				layerBitMap[y][x>>3] |= (1 << (x&7));

			// draw line that increments along X
			while (y < yend)
			{
				y++;
				if (d < 0) d += incr1; else
				{
					x += xincr;   d += incr2;
				}
				if (i == 7) i = 0; else i++;
				if ((pattern & (1 << i)) == 0) continue;
				if (layerBitMap == null) opaqueData[y * sz.width + x] = col.getRGB(); else
					layerBitMap[y][x>>3] |= (1 << (x&7));
			}
		}
	}

	private void drawThickLine(int x1, int y1, int x2, int y2, byte [][] layerBitMap, EGraphics desc)
	{
		// get color and pattern information
		Color col = Color.BLACK;
		if (desc != null) col = desc.getColor();

		// initialize the Bresenham algorithm
		int dx = Math.abs(x2-x1);
		int dy = Math.abs(y2-y1);
		if (dx > dy)
		{
			// initialize for lines that increment along X
			int incr1 = 2 * dy;
			int incr2 = 2 * (dy - dx);
			int d = incr2;
			int x, y, xend, yend, yincr;
			if (x1 > x2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (yend < y) yincr = -1; else yincr = 1;
			drawThickPoint(x, y, layerBitMap, col);

			// draw line that increments along X
			while (x < xend)
			{
				x++;
				if (d < 0) d += incr1; else
				{
					y += yincr;
					d += incr2;
				}
				drawThickPoint(x, y, layerBitMap, col);
			}
		} else
		{
			// initialize for lines that increment along Y
			int incr1 = 2 * dx;
			int incr2 = 2 * (dx - dy);
			int d = incr2;
			int x, y, xend, yend, xincr;
			if (y1 > y2)
			{
				x = x2;   y = y2;   xend = x1;   yend = y1;
			} else
			{
				x = x1;   y = y1;   xend = x2;   yend = y2;
			}
			if (xend < x) xincr = -1; else xincr = 1;
			drawThickPoint(x, y, layerBitMap, col);

			// draw line that increments along X
			while (y < yend)
			{
				y++;
				if (d < 0) d += incr1; else
				{
					x += xincr;
					d += incr2;
				}
				drawThickPoint(x, y, layerBitMap, col);
			}
		}
	}

	/****************************** POLYGON DRAWING ******************************/

	/*
	 * Method to draw a polygon on the off-screen buffer.
	 */
	private void drawPolygon(Point [] points, byte [][] layerBitMap, EGraphics desc)
	{
		// get color and pattern information
		Color col = Color.BLACK;
		int [] pattern = null;
		if (desc != null)
		{
			col = desc.getColor();
			if (desc.isPatternedOnDisplay())
				pattern = desc.getPattern();
		}

		// fill in internal structures
		PolySeg edgelist = null;
		PolySeg [] polySegs = new PolySeg[points.length];
		for(int i=0; i<points.length; i++)
		{
			polySegs[i] = new PolySeg();
			if (i == 0)
			{
				polySegs[i].fx = points[points.length-1].x;
				polySegs[i].fy = points[points.length-1].y;
			} else
			{
				polySegs[i].fx = points[i-1].x;
				polySegs[i].fy = points[i-1].y;
			}
			polySegs[i].tx = points[i].x;   polySegs[i].ty = points[i].y;
		}
		for(int i=0; i<points.length; i++)
		{
			// draw the edge lines to make the polygon clean
			if (pattern != null && desc.isOutlinePatternedOnDisplay())
				drawSolidLine(polySegs[i].fx, polySegs[i].fy, polySegs[i].tx, polySegs[i].ty, layerBitMap, desc);

			// compute the direction of this edge
			int j = polySegs[i].ty - polySegs[i].fy;
			if (j > 0) polySegs[i].direction = 1; else
				if (j < 0) polySegs[i].direction = -1; else
					polySegs[i].direction = 0;

			// compute the X increment of this edge
			if (j == 0) polySegs[i].increment = 0; else
			{
				polySegs[i].increment = polySegs[i].tx - polySegs[i].fx;
				if (polySegs[i].increment != 0) polySegs[i].increment =
					(polySegs[i].increment * 65536 - j + 1) / j;
			}
			polySegs[i].tx <<= 16;   polySegs[i].fx <<= 16;

			// make sure "from" is above "to"
			if (polySegs[i].fy > polySegs[i].ty)
			{
				j = polySegs[i].tx;
				polySegs[i].tx = polySegs[i].fx;
				polySegs[i].fx = j;
				j = polySegs[i].ty;
				polySegs[i].ty = polySegs[i].fy;
				polySegs[i].fy = j;
			}

			// insert this edge into the edgelist, sorted by ascending "fy"
			if (edgelist == null)
			{
				edgelist = polySegs[i];
				polySegs[i].nextedge = null;
			} else
			{
				// insert by ascending "fy"
				if (edgelist.fy > polySegs[i].fy)
				{
					polySegs[i].nextedge = edgelist;
					edgelist = polySegs[i];
				} else for(PolySeg a = edgelist; a != null; a = a.nextedge)
				{
					if (a.nextedge == null ||
						a.nextedge.fy > polySegs[i].fy)
					{
						// insert after this
						polySegs[i].nextedge = a.nextedge;
						a.nextedge = polySegs[i];
						break;
					}
				}
			}
		}

		// scan polygon and render
		int ycur = 0;
		PolySeg active = null;
		while (active != null || edgelist != null)
		{
			if (active == null)
			{
				active = edgelist;
				active.nextactive = null;
				edgelist = edgelist.nextedge;
				ycur = active.fy;
			}

			// introduce edges from edge list into active list
			while (edgelist != null && edgelist.fy <= ycur)
			{
				// insert "edgelist" into active list, sorted by "fx" coordinate
				if (active.fx > edgelist.fx ||
					(active.fx == edgelist.fx && active.increment > edgelist.increment))
				{
					edgelist.nextactive = active;
					active = edgelist;
					edgelist = edgelist.nextedge;
				} else for(PolySeg a = active; a != null; a = a.nextactive)
				{
					if (a.nextactive == null ||
						a.nextactive.fx > edgelist.fx ||
							(a.nextactive.fx == edgelist.fx &&
								a.nextactive.increment > edgelist.increment))
					{
						// insert after this
						edgelist.nextactive = a.nextactive;
						a.nextactive = edgelist;
						edgelist = edgelist.nextedge;
						break;
					}
				}
			}

			// generate regions to be filled in on current scan line
			int wrap = 0;
			PolySeg left = active;
			for(PolySeg edge = active; edge != null; edge = edge.nextactive)
			{
				wrap = wrap + edge.direction;
				if (wrap == 0)
				{
					int j = (left.fx + 32768) >> 16;
					int k = (edge.fx + 32768) >> 16;

					if (pattern != null)
					{
						int pat = pattern[ycur & 15];
						if (pat != 0)
						{
							if (layerBitMap == null)
							{
								int baseIndex = ycur * sz.width;
								for(int x=j; x<=k; x++)
								{
									if ((pat & (1 << (15-(x&15)))) != 0)
										opaqueData[baseIndex + x] = col.getRGB();
								}
							} else
							{
								byte [] row = layerBitMap[ycur];
								for(int x=j; x<=k; x++)
								{
									if ((pat & (1 << (15-(x&15)))) != 0)
										row[x>>3] |= (1 << (x&7));
								}
							}
						}
					} else
					{
						if (layerBitMap == null)
						{
							int baseIndex = ycur * sz.width;
							for(int x=j; x<=k; x++)
							{
								opaqueData[baseIndex + x] = col.getRGB();
							}
						} else
						{
							byte [] row = layerBitMap[ycur];
							for(int x=j; x<=k; x++)
							{
								row[x>>3] |= (1 << (x&7));
							}
						}
					}
					left = edge.nextactive;
				}
			}
			ycur++;

			// update edges in active list
			PolySeg lastedge = null;
			for(PolySeg edge = active; edge != null; edge = edge.nextactive)
			{
				if (ycur >= edge.ty)
				{
					if (lastedge == null) active = edge.nextactive;
						else lastedge.nextactive = edge.nextactive;
				} else
				{
					edge.fx += edge.increment;
					lastedge = edge;
				}
			}
		}
	}

	/****************************** BOX DRAWING ******************************/

	/*
	 * Method to draw a box on the off-screen buffer.
	 */
	void drawBox(int lX, int hX, int lY, int hY, byte [][] layerBitMap, EGraphics desc)
	{
		// get color and pattern information
		Color col = Color.BLACK;
		int [] pattern = null;
		if (desc != null)
		{
			col = desc.getColor();
			if (desc.isPatternedOnDisplay())
				pattern = desc.getPattern();
		}

		// run through each row
		for(int y=lY; y<=hY; y++)
		{
			// setup pattern for this row
			int pat = 0;
			if (pattern != null)
			{
				pat = pattern[y&15];
				if (pat == 0) continue;
			}

			// setup pointers for filling this row
			byte [] row = null;
			int baseIndex = 0;
			if (layerBitMap == null) baseIndex = y * sz.width; else
				row = layerBitMap[y];

			// process the row
			for(int x=lX; x<= hX; x++)
			{
				// ignore if not in the pattern
				if (pattern != null && (pat & (0x8000 >> (x&15))) == 0) continue;

				// render the pixel
				if (layerBitMap == null) opaqueData[baseIndex + x] = col.getRGB();  else
					row[x>>3] |= (1 << (x&7));
			}
		}
	}

	/****************************** TEXT DRAWING ******************************/

	/*
	 * Method to draw a text on the off-screen buffer
	 */
	public void drawText(Rectangle rect, Poly.Type style, TextDescriptor descript, String s, byte [][] layerBitMap, EGraphics desc)
	{
		// get parameters
		// quit if string is null
		int len = s.length();
		if (len == 0) return;

		// get parameters
		Color col = Color.BLACK;
		if (desc != null) col = desc.getColor();

		// get text description
		int size = 14;
		int fontStyle = Font.PLAIN;
		String fontName = "SansSerif";
		boolean italic = false;
		boolean bold = false;
		int rotation = 0;
		if (descript != null)
		{
			size = descript.getTrueSize(world);
			if (size <= 0) size = 1;
			italic = descript.isItalic();
			bold = descript.isBold();
			int fontIndex = descript.getFace();
			if (fontIndex != 0)
			{
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontIndex);
				if (af != null) fontName = af.getName();
			}
		}
		Raster ras = renderText(s, fontName, size, italic, bold);
		Point pt = getTextCorner(ras, style, rect);
		int atX = pt.x;
		int atY = pt.y - ras.getHeight();
		DataBufferByte dbb = (DataBufferByte)ras.getDataBuffer();
		byte [] samples = dbb.getData();

		switch (rotation)
		{
			case 0:			// no rotation
				// copy text buffer to the main offscreen buffer
				int sx = 0;
				if (atX < 0) sx = -atX;
				int ex = ras.getWidth();
				if (atX+ras.getWidth() >= sz.width) ex = sz.width-1 - atX;
				for(int y=0; y<ras.getHeight(); y++)
				{
					int trueY = atY + y;
					if (trueY < 0 || trueY >= sz.height) continue;

					// setup pointers for filling this row
					byte [] row = null;
					int baseIndex = 0;
					if (layerBitMap == null) baseIndex = trueY * sz.width; else
						row = layerBitMap[trueY];
					int samp = y * ras.getWidth() + sx;
					for(int x=sx; x<ex; x++)
					{
						int trueX = atX + x;
						if (samples[samp++] == 0) continue;
						if (layerBitMap == null) opaqueData[baseIndex + trueX] = col.getRGB();  else
							row[trueX>>3] |= (1 << (trueX&7));
					}
				}
				break;
//			case 1:			// 90 degrees counterclockwise
//				if (atX-ras.getHeight() < lx) sx = lx+ras.getHeight()-atX; else sx = 0;
//				if (atX >= hx) ex = hx - atX; else ex = ras.getHeight();
//				pos = atY - ras.getWidth();
//				for(i=0; i<ras.getWidth(); i++)
//				{
//					desti = pos + i;
//					if (desti < ly || desti > hy) continue;
//					dest = wf->rowstart[desti];
//					for(j=sx; j<ex; j++)
//					{
//						ptr = rowstart[ras.getHeight()-1-j];
//						if (ptr[ras.getWidth()-1-i] == 0) continue;
//						dpos = atX - j;
//						dest[dpos] = (UCHAR1)((dest[dpos] & mask) | col);
//					}
//				}
//				break;
//			case 2:			// 180 degrees
//				if (atX-ras.getWidth() < lx) sx = lx+ras.getWidth()-atX; else sx = 0;
//				if (atX >= hx) ex = hx - atX; else ex = ras.getWidth();
//				pos = atY;
//				for(i=0; i<ras.getHeight(); i++)
//				{
//					desti = pos + ras.getHeight() - i;
//					if (desti < ly || desti > hy) continue;
//					ptr = rowstart[i];
//					dest = wf->rowstart[desti];
//					for(j=sx; j<ex; j++)
//					{
//						if (ptr[j] == 0) continue;
//						dpos = atX - j;
//						dest[dpos] = (UCHAR1)((dest[dpos] & mask) | col);
//					}
//				}
//				break;
//			case 3:			// 90 degrees clockwise
//				if (atX < lx) sx = lx-atX; else sx = 0;
//				if (atX+ras.getHeight() >= hx) ex = hx - atX; else ex = ras.getHeight();
//				pos = atY;
//				for(i=0; i<ras.getWidth(); i++)
//				{
//					desti = pos + i;
//					if (desti < ly || desti > hy) continue;
//					dest = wf->rowstart[desti];
//					for(j=sx; j<ex; j++)
//					{
//						ptr = rowstart[ras.getHeight()-1-j];
//						if (ptr[i] == 0) continue;
//						dpos = atX + j;
//						dest[dpos] = (UCHAR1)((dest[dpos] & mask) | col);
//					}
//				}
//				break;
		}
	}

	/**
	 * Method to return the coordinates of the lower-left corner of text in this window.
	 * @param gv the GlyphVector describing the text.
	 * @param style the grab-point information about where the text is anchored.
	 * @param lX the low X bound of the polygon containing the text.
	 * @param hX the high X bound of the polygon containing the text.
	 * @param lY the low Y bound of the polygon containing the text.
	 * @param hY the high Y bound of the polygon containing the text.
	 * @return the coordinates of the lower-left corner of the text.
	 */
	private Point getTextCorner(Raster ras, Poly.Type style, Rectangle rect)
	{
		// adjust to place text in the center
		int cX = (int)rect.getCenterX();
		int cY = (int)rect.getCenterY();
		int textWidth = ras.getWidth();
		int textHeight = ras.getHeight();
		if (style == Poly.Type.TEXTCENT)
		{
			cX -= textWidth/2;
			cY += textHeight/2;
		} else if (style == Poly.Type.TEXTTOP)
		{
			cX -= textWidth/2;
			cY -= textHeight;
		} else if (style == Poly.Type.TEXTBOT)
		{
			cX -= textWidth/2;
		} else if (style == Poly.Type.TEXTLEFT)
		{
			cY += textHeight/2;
		} else if (style == Poly.Type.TEXTRIGHT)
		{
			cX -= textWidth;
			cY += textHeight/2;
		} else if (style == Poly.Type.TEXTTOPLEFT)
		{
			cY -= textHeight;
		} else if (style == Poly.Type.TEXTBOTLEFT)
		{
		} else if (style == Poly.Type.TEXTTOPRIGHT)
		{
			cX -= textWidth;
			cY -= textHeight;
		} else if (style == Poly.Type.TEXTBOTRIGHT)
		{
			cX -= textWidth;
		} if (style == Poly.Type.TEXTBOX)
		{
//			if (textWidth > rect.getWidth())
//			{
//				// text too big for box: scale it down
//				textScale *= rect.getWidth() / textWidth;
//			}
			cX -= textWidth/2;
			cY += textHeight/2;
		}
		return new Point(cX, cY);
	}

	public static Raster renderText(String msg, String font, int tsize, boolean italic, boolean bold)
	{
		// get the font
		int fontStyle = Font.PLAIN;
		if (italic) fontStyle |= Font.ITALIC;
		if (bold) fontStyle |= Font.BOLD;
		Font theFont = new Font(font, fontStyle, tsize);
		if (theFont == null)
		{
			System.out.println("Could not find the proper font");
			return null;
		}

		// convert the text to a GlyphVector
		FontRenderContext frc = new FontRenderContext(null, false, false);
		GlyphVector gv = theFont.createGlyphVector(frc, msg);

		// allocate space for the rendered text
		Rectangle rect = gv.getPixelBounds(frc, 0, 0);
		BufferedImage textImage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_BYTE_GRAY);

		// now render it
		Graphics2D g2 = (Graphics2D)textImage.getGraphics();
		g2.setColor(new Color(128,128,128));
		g2.drawGlyphVector(gv, (float)-rect.x, (float)-rect.y);

		// return the bits
		return textImage.getData();
	}


	/****************************** CIRCLE DRAWING ******************************/

	/*
	 * Method to draw a circle on the off-screen buffer
	 */
	private void drawCircle(Point center, Point edge, byte [][] layerBitMap, EGraphics desc)
	{
		// get parameters
		int radius = (int)center.distance(edge);
		Color col = Color.BLACK;
		if (desc != null) col = desc.getColor();

		// set redraw area
		int left = center.x - radius;
		int right = center.x + radius + 1;
		int top = center.y - radius;
		int bottom = center.y + radius + 1;

		int x = 0;   int y = radius;
		int d = 3 - 2 * radius;
		if (left >= 0 && right < sz.width && top >= 0 && bottom < sz.height)
		{
			// no clip version is faster
			while (x <= y)
			{
				if (layerBitMap == null)
				{
					int baseIndex = (center.y + y) * sz.width;
					opaqueData[baseIndex + (center.x+x)] = col.getRGB();
					opaqueData[baseIndex + (center.x-x)] = col.getRGB();

					baseIndex = (center.y - y) * sz.width;
					opaqueData[baseIndex + (center.x+x)] = col.getRGB();
					opaqueData[baseIndex + (center.x-x)] = col.getRGB();

					baseIndex = (center.y + x) * sz.width;
					opaqueData[baseIndex + (center.x+y)] = col.getRGB();
					opaqueData[baseIndex + (center.x-y)] = col.getRGB();

					baseIndex = (center.y - x) * sz.width;
					opaqueData[baseIndex + (center.x+y)] = col.getRGB();
					opaqueData[baseIndex + (center.x-y)] = col.getRGB();
				} else
				{
					byte [] row = layerBitMap[center.y + y];
					row[(center.x+x)>>3] |= (1 << ((center.x+x)&7));
					row[(center.x-x)>>3] |= (1 << ((center.x-x)&7));

					row = layerBitMap[center.y - y];
					row[(center.x+x)>>3] |= (1 << ((center.x+x)&7));
					row[(center.x-x)>>3] |= (1 << ((center.x-x)&7));

					row = layerBitMap[center.y + x];
					row[(center.x+y)>>3] |= (1 << ((center.x+y)&7));
					row[(center.x-y)>>3] |= (1 << ((center.x-y)&7));

					row = layerBitMap[center.y - x];
					row[(center.x+y)>>3] |= (1 << ((center.x+y)&7));
					row[(center.x-y)>>3] |= (1 << ((center.x-y)&7));
				}

				if (d < 0) d += 4*x + 6; else
				{
					d += 4 * (x-y) + 10;
					y--;
				}
				x++;
			}
		} else
		{
			// clip version
			while (x <= y)
			{
				int thisy = center.y + y;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + x;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - x;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
				}

				thisy = center.y - y;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + x;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - x;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
				}

				thisy = center.y + x;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + y;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - y;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
				}

				thisy = center.y - x;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + y;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - y;
					if (thisx >= 0 && thisx < sz.width)
						drawPoint(thisx, thisy, layerBitMap, col);
				}

				if (d < 0) d += 4*x + 6; else
				{
					d += 4 * (x-y) + 10;
					y--;
				}
				x++;
			}
		}
	}

	/*
	 * Method to draw a thick circle on the off-screen buffer
	 */
	private void drawThickCircle(Point center, Point edge, byte [][] layerBitMap, EGraphics desc)
	{
		// get parameters
		int radius = (int)center.distance(edge);
		Color col = Color.BLACK;
		if (desc != null) col = desc.getColor();

		// set redraw area
		int left = center.x - radius;
		int right = center.x + radius + 1;
		int top = center.y - radius;
		int bottom = center.y + radius + 1;

		int x = 0;   int y = radius;
		int d = 3 - 2 * radius;
//		if (left >= 0 && right < maxx && top >= 0 && bottom < maxy)
//		{
//			// no clip version is faster
//			while (x <= y)
//			{
//				// draw basic circle
//				thisrow1 = wf->rowstart[aty + y];
//				thisrow2 = wf->rowstart[aty - y];
//				thisrow3 = wf->rowstart[aty + x];
//				thisrow4 = wf->rowstart[aty - x];
//				thisrow1[atx + x] = (UCHAR1)((thisrow1[atx + x] & mask) | col);
//				thisrow1[atx - x] = (UCHAR1)((thisrow1[atx - x] & mask) | col);
//
//				thisrow2[atx + x] = (UCHAR1)((thisrow2[atx + x] & mask) | col);
//				thisrow2[atx - x] = (UCHAR1)((thisrow2[atx - x] & mask) | col);
//
//				thisrow3[atx + y] = (UCHAR1)((thisrow3[atx + y] & mask) | col);
//				thisrow3[atx - y] = (UCHAR1)((thisrow3[atx - y] & mask) | col);
//
//				thisrow4[atx + y] = (UCHAR1)((thisrow4[atx + y] & mask) | col);
//				thisrow4[atx - y] = (UCHAR1)((thisrow4[atx - y] & mask) | col);
//
//				// draw 1 pixel around it to make it thick
//				thisrow1m = wf->rowstart[aty + y - 1];
//				thisrow2m = wf->rowstart[aty - y - 1];
//				thisrow3m = wf->rowstart[aty + x - 1];
//				thisrow4m = wf->rowstart[aty - x - 1];
//				thisrow1p = wf->rowstart[aty + y + 1];
//				thisrow2p = wf->rowstart[aty - y + 1];
//				thisrow3p = wf->rowstart[aty + x + 1];
//				thisrow4p = wf->rowstart[aty - x + 1];
//
//				thisrow1[atx + x + 1] = (UCHAR1)((thisrow1[atx + x + 1] & mask) | col);
//				thisrow1[atx + x - 1] = (UCHAR1)((thisrow1[atx + x - 1] & mask) | col);
//				thisrow1[atx - x + 1] = (UCHAR1)((thisrow1[atx - x + 1] & mask) | col);
//				thisrow1[atx - x - 1] = (UCHAR1)((thisrow1[atx - x - 1] & mask) | col);
//				thisrow1m[atx + x] = (UCHAR1)((thisrow1m[atx + x] & mask) | col);
//				thisrow1m[atx - x] = (UCHAR1)((thisrow1m[atx - x] & mask) | col);
//				thisrow1p[atx + x] = (UCHAR1)((thisrow1p[atx + x] & mask) | col);
//				thisrow1p[atx - x] = (UCHAR1)((thisrow1p[atx - x] & mask) | col);
//
//				thisrow2[atx + x + 1] = (UCHAR1)((thisrow2[atx + x + 1] & mask) | col);
//				thisrow2[atx + x - 1] = (UCHAR1)((thisrow2[atx + x - 1] & mask) | col);
//				thisrow2[atx - x + 1] = (UCHAR1)((thisrow2[atx - x + 1] & mask) | col);
//				thisrow2[atx - x - 1] = (UCHAR1)((thisrow2[atx - x - 1] & mask) | col);
//				thisrow2m[atx + x] = (UCHAR1)((thisrow2m[atx + x] & mask) | col);
//				thisrow2m[atx - x] = (UCHAR1)((thisrow2m[atx - x] & mask) | col);
//				thisrow2p[atx + x] = (UCHAR1)((thisrow2p[atx + x] & mask) | col);
//				thisrow2p[atx - x] = (UCHAR1)((thisrow2p[atx - x] & mask) | col);
//
//				thisrow3[atx + y + 1] = (UCHAR1)((thisrow3[atx + y + 1] & mask) | col);
//				thisrow3[atx + y - 1] = (UCHAR1)((thisrow3[atx + y - 1] & mask) | col);
//				thisrow3[atx - y + 1] = (UCHAR1)((thisrow3[atx - y + 1] & mask) | col);
//				thisrow3[atx - y - 1] = (UCHAR1)((thisrow3[atx - y - 1] & mask) | col);
//				thisrow3m[atx + y] = (UCHAR1)((thisrow3m[atx + y] & mask) | col);
//				thisrow3m[atx - y] = (UCHAR1)((thisrow3m[atx - y] & mask) | col);
//				thisrow3p[atx + y] = (UCHAR1)((thisrow3p[atx + y] & mask) | col);
//				thisrow3p[atx - y] = (UCHAR1)((thisrow3p[atx - y] & mask) | col);
//
//				thisrow4[atx + y + 1] = (UCHAR1)((thisrow4[atx + y + 1] & mask) | col);
//				thisrow4[atx + y - 1] = (UCHAR1)((thisrow4[atx + y - 1] & mask) | col);
//				thisrow4[atx - y + 1] = (UCHAR1)((thisrow4[atx - y + 1] & mask) | col);
//				thisrow4[atx - y - 1] = (UCHAR1)((thisrow4[atx - y - 1] & mask) | col);
//				thisrow4m[atx + y] = (UCHAR1)((thisrow4m[atx + y] & mask) | col);
//				thisrow4m[atx - y] = (UCHAR1)((thisrow4m[atx - y] & mask) | col);
//				thisrow4p[atx + y] = (UCHAR1)((thisrow4p[atx + y] & mask) | col);
//				thisrow4p[atx - y] = (UCHAR1)((thisrow4p[atx - y] & mask) | col);
//
//				if (d < 0) d += 4*x + 6; else
//				{
//					d += 4 * (x-y) + 10;
//					y--;
//				}
//				x++;
//			}
//		} else
		{
			// clip version
			while (x <= y)
			{
				int thisy = center.y + y;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + x;
					if (thisx >= 0 && thisx < sz.width)
						drawThickPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - x;
					if (thisx >= 0 && thisx < sz.width)
						drawThickPoint(thisx, thisy, layerBitMap, col);
				}

				thisy = center.y - y;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + x;
					if (thisx >= 0 && thisx < sz.width)
						drawThickPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - x;
					if (thisx >= 0 && thisx < sz.width)
						drawThickPoint(thisx, thisy, layerBitMap, col);
				}

				thisy = center.y + x;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + y;
					if (thisx >= 0 && thisx < sz.width)
						drawThickPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - y;
					if (thisx >= 0 && thisx < sz.width)
						drawThickPoint(thisx, thisy, layerBitMap, col);
				}

				thisy = center.y - x;
				if (thisy >= 0 && thisy < sz.height)
				{
					int thisx = center.x + y;
					if (thisx >= 0 && thisx < sz.width)
						drawThickPoint(thisx, thisy, layerBitMap, col);
					thisx = center.x - y;
					if (thisx >= 0 && thisx < sz.width)
						drawThickPoint(thisx, thisy, layerBitMap, col);
				}

				if (d < 0) d += 4*x + 6; else
				{
					d += 4 * (x-y) + 10;
					y--;
				}
				x++;
			}
		}
	}

	/****************************** DISC DRAWING ******************************/

	/*
	 * Method to draw a scan line of the filled-in circle of radius "radius"
	 */
	private void drawDiscRow(int thisy, int startx, int endx, byte [][] layerBitMap, Color col, int [] pattern)
	{
		if (thisy < 0 || thisy >= sz.height) return;
		if (startx < 0) startx = 0;
		if (endx >= sz.width) endx = sz.width - 1;
		if (pattern != null)
		{
			int pat = pattern[thisy & 15];
			if (pat != 0)
			{
				if (layerBitMap == null)
				{
					int baseIndex = thisy * sz.width;
					for(int x=startx; x<=endx; x++)
					{
						if ((pat & (1 << (15-(x&15)))) != 0)
							opaqueData[baseIndex + x] = col.getRGB();
					}
				} else
				{
					byte [] row = layerBitMap[thisy];
					for(int x=startx; x<=endx; x++)
					{
						if ((pat & (1 << (15-(x&15)))) != 0)
							row[x>>3] |= (1 << (x&7));
					}
				}
			}
		} else
		{
			if (layerBitMap == null)
			{
				int baseIndex = thisy * sz.width;
				for(int x=startx; x<=endx; x++)
				{
					opaqueData[baseIndex + x] = col.getRGB();
				}
			} else
			{
				byte [] row = layerBitMap[thisy];
				for(int x=startx; x<=endx; x++)
				{
					row[x>>3] |= (1 << (x&7));
				}
			}
		}
	}

	/*
	 * Method to draw a filled-in circle of radius "radius" on the off-screen buffer
	 */
	private void drawDisc(Point center, Point edge, byte [][] layerBitMap, EGraphics desc)
	{
		// get parameters
		int radius = (int)center.distance(edge);
		Color col = Color.BLACK;
		int [] pattern = null;
		if (desc != null)
		{
			col = desc.getColor();
			if (desc.isPatternedOnDisplay())
				pattern = desc.getPattern();
		}

		// set redraw area
		int left = center.x - radius;
		int right = center.x + radius + 1;
		int top = center.y - radius;
		int bottom = center.y + radius + 1;

		if (radius == 1)
		{
			// just fill the area for discs this small
			if (left < 0) left = 0;
			if (right >= sz.width) right = sz.width - 1;
			for(int y=top; y<bottom; y++)
			{
				if (y < 0 || y >= sz.height) continue;
				for(int x=left; x<right; x++)
					drawPoint(x, y, layerBitMap, col);
			}
			return;
		}

		int x = 0;   int y = radius;
		int d = 3 - 2 * radius;
		while (x <= y)
		{
			drawDiscRow(center.y+y, center.x-x, center.x+x, layerBitMap, col, pattern);
			drawDiscRow(center.y-y, center.x-x, center.x+x, layerBitMap, col, pattern);
			drawDiscRow(center.y+x, center.x-y, center.x+y, layerBitMap, col, pattern);
			drawDiscRow(center.y-x, center.x-y, center.x+y, layerBitMap, col, pattern);

			if (d < 0) d += 4*x + 6; else
			{
				d += 4 * (x-y) + 10;
				y--;
			}
			x++;
		}
	}

	/****************************** ARC DRAWING ******************************/

	private boolean [] arcOctTable = new boolean[9];
	private Point      arcCenter;
	private int        arcRadius;
	private Color      arcCol;
	private byte [][]  arcLayerBitMap;
	private boolean    arcThick;

	private int arcFindOctant(int x, int y)
	{
		if (x > 0)
		{
			if (y >= 0)
			{
				if (y >= x) return 7;
				return 8;
			}
			if (x >= -y) return 1;
			return 2;
		}
		if (y > 0)
		{
			if (y > -x) return 6;
			return 5;
		}
		if (y > x) return 4;
		return 3;
	}

	private Point arcXformOctant(int x, int y, int oct)
	{
		switch (oct)
		{
			case 1 : return new Point(-y,  x);
			case 2 : return new Point( x, -y);
			case 3 : return new Point(-x, -y);
			case 4 : return new Point(-y, -x);
			case 5 : return new Point( y, -x);
			case 6 : return new Point(-x,  y);
			case 7 : return new Point( x,  y);
			case 8 : return new Point( y,  x);
		}
		return null;
	}

	private void arcDoPixel(int x, int y)
	{
		if (x < 0 || x >= sz.width || y < 0 || y >= sz.height) return;
		if (arcThick)
		{
			drawThickPoint(x, y, arcLayerBitMap, arcCol);
		} else
		{
			drawPoint(x, y, arcLayerBitMap, arcCol);
		}
	}

	private void arcOutXform(int x, int y)
	{
		if (arcOctTable[1]) arcDoPixel( y + arcCenter.x, -x + arcCenter.y);
		if (arcOctTable[2]) arcDoPixel( x + arcCenter.x, -y + arcCenter.y);
		if (arcOctTable[3]) arcDoPixel(-x + arcCenter.x, -y + arcCenter.y);
		if (arcOctTable[4]) arcDoPixel(-y + arcCenter.x, -x + arcCenter.y);
		if (arcOctTable[5]) arcDoPixel(-y + arcCenter.x,  x + arcCenter.y);
		if (arcOctTable[6]) arcDoPixel(-x + arcCenter.x,  y + arcCenter.y);
		if (arcOctTable[7]) arcDoPixel( x + arcCenter.x,  y + arcCenter.y);
		if (arcOctTable[8]) arcDoPixel( y + arcCenter.x,  x + arcCenter.y);
	}

	private void arcBresCW(Point pt, Point pt1)
	{
		int d = 3 - 2 * pt.y + 4 * pt.x;
		while (pt.x < pt1.x && pt.y > pt1.y)
		{
			arcOutXform(pt.x, pt.y);
			if (d < 0) d += 4 * pt.x + 6; else
			{
				d += 4 * (pt.x-pt.y) + 10;
				pt.y--;
			}
			pt.x++;
		}

		// get to the end
		for ( ; pt.x < pt1.x; pt.x++) arcOutXform(pt.x, pt.y);
		for ( ; pt.y > pt1.y; pt.y--) arcOutXform(pt.x, pt.y);
		arcOutXform(pt1.x, pt1.y);
	}

	private void arcBresMidCW(Point pt)
	{
		int d = 3 - 2 * pt.y + 4 * pt.x;
		while (pt.x < pt.y)
		{
			arcOutXform(pt.x, pt.y);
			if (d < 0) d += 4 * pt.x + 6; else
			{
				d += 4 * (pt.x-pt.y) + 10;
				pt.y--;
			}
			pt.x++;
	   }
	   if (pt.x == pt.y) arcOutXform(pt.x, pt.y);
	}

	private void arcBresMidCCW(Point pt)
	{
		int d = 3 + 2 * pt.y - 4 * pt.x;
		while (pt.x > 0)
		{
			arcOutXform(pt.x, pt.y);
			if (d > 0) d += 6-4 * pt.x; else
			{
				d += 4 * (pt.y-pt.x) + 10;
				pt.y++;
			}
			pt.x--;
	   }
	   arcOutXform(0, arcRadius);
	}

	private void arcBresCCW(Point pt, Point pt1)
	{
		int d = 3 + 2 * pt.y + 4 * pt.x;
		while(pt.x > pt1.x && pt.y < pt1.y)
		{
			// not always correct
			arcOutXform(pt.x, pt.y);
			if (d > 0) d += 6 - 4 * pt.x; else
			{
				d += 4 * (pt.y-pt.x) + 10;
				pt.y++;
			}
			pt.x--;
		}

		// get to the end
		for ( ; pt.x > pt1.x; pt.x--) arcOutXform(pt.x, pt.y);
		for ( ; pt.y < pt1.y; pt.y++) arcOutXform(pt.x, pt.y);
		arcOutXform(pt1.x, pt1.y);
	}

	/*
	 * draws an arc centered at (centerx, centery), clockwise,
	 * passing by (x1,y1) and (x2,y2)
	 */
	private void drawCircleArc(Point center, Point p1, Point p2, boolean thick, byte [][] layerBitMap, EGraphics desc)
	{
		// ignore tiny arcs
		if (p1.x == p2.x && p1.y == p2.y) return;

		// get parameters
		arcLayerBitMap = layerBitMap;
		arcCol = Color.BLACK;
		if (desc != null) arcCol = desc.getColor();

		arcCenter = center;
		int pa_x = p2.x - arcCenter.x;
		int pa_y = p2.y - arcCenter.y;
		int pb_x = p1.x - arcCenter.x;
		int pb_y = p1.y - arcCenter.y;
		arcRadius = (int)arcCenter.distance(p2);
		int alternate = (int)arcCenter.distance(p1);
		int start_oct = arcFindOctant(pa_x, pa_y);
		int end_oct   = arcFindOctant(pb_x, pb_y);
		arcThick = thick;

		// move the point
		if (arcRadius != alternate)
		{
			int diff = arcRadius-alternate;
			switch (end_oct)
			{
				case 6:
				case 7: /*  y >  x */ pb_y += diff;  break;
				case 8: /*  x >  y */
				case 1: /*  x > -y */ pb_x += diff;  break;
				case 2: /* -y >  x */
				case 3: /* -y > -x */ pb_y -= diff;  break;
				case 4: /* -y < -x */
				case 5: /*  y < -x */ pb_x -= diff;  break;
			}
		}

		for(int i=1; i<9; i++) arcOctTable[i] = false;

		if (start_oct == end_oct)
		{
			arcOctTable[start_oct] = true;
			Point pa = arcXformOctant(pa_x, pa_y, start_oct);
			Point pb = arcXformOctant(pb_x, pb_y, start_oct);

			if ((start_oct&1) != 0) arcBresCW(pa, pb);
			else                    arcBresCCW(pa, pb);
			arcOctTable[start_oct] = false;
		} else
		{
			arcOctTable[start_oct] = true;
			Point pt = arcXformOctant(pa_x, pa_y, start_oct);
			if ((start_oct&1) != 0) arcBresMidCW(pt);
			else			    	arcBresMidCCW(pt);
			arcOctTable[start_oct] = false;

			arcOctTable[end_oct] = true;
			pt = arcXformOctant(pb_x, pb_y, end_oct);
			if ((end_oct&1) != 0) arcBresMidCCW(pt);
			else			      arcBresMidCW(pt);
			arcOctTable[end_oct] = false;

			if (MODP(start_oct+1) != end_oct)
			{
				if (MODP(start_oct+1) == MODM(end_oct-1))
				{
					arcOctTable[MODP(start_oct+1)] = true;
				} else
				{
					for(int i = MODP(start_oct+1); i != end_oct; i = MODP(i+1))
						arcOctTable[i] = true;
				}
				arcBresMidCW(new Point(0, arcRadius));
			}
		}
	}

	private int MODM(int x) { return (x<1) ? x+8 : x; }
	private int MODP(int x) { return (x>8) ? x-8 : x; }

	/****************************** SUPPORT ******************************/

	private void drawPoint(int x, int y, byte [][] layerBitMap, Color col)
	{
		if (layerBitMap == null)
		{
			opaqueData[y * sz.width + x] = col.getRGB();
		} else
		{
			layerBitMap[y][x>>3] |= (1 << (x&7));
		}
	}

	private void drawThickPoint(int x, int y, byte [][] layerBitMap, Color col)
	{
		if (layerBitMap == null)
		{
			opaqueData[y * sz.width + x] = col.getRGB();
			if (x > 0)
				opaqueData[y * sz.width + (x-1)] = col.getRGB();
			if (x < sz.width-1)
				opaqueData[y * sz.width + (x+1)] = col.getRGB();
			if (y > 0)
				opaqueData[(y-1) * sz.width + (x+1)] = col.getRGB();
			if (y < sz.height-1)
				opaqueData[(y+1) * sz.width + (x+1)] = col.getRGB();
		} else
		{
			layerBitMap[y][x>>3] |= (1 << (x&7));
			if (x > 0)
				layerBitMap[y][(x-1)>>3] |= (1 << (x&7));
			if (x < sz.width-1)
				layerBitMap[y][(x+1)>>3] |= (1 << (x&7));
			if (y > 0)
				layerBitMap[y-1][x>>3] |= (1 << (x&7));
			if (y < sz.height-1)
				layerBitMap[y+1][x>>3] |= (1 << (x&7));
		}
	}

	/****************************** CLIPPING ******************************/

	// clipping directions
	private static final int LEFT   = 1;
	private static final int RIGHT  = 2;
	private static final int BOTTOM = 4;
	private static final int TOP    = 8;

	/*
	 * Method to clip a line from (fx,fy) to (tx,ty) in the rectangle lx <= X <= hx and ly <= Y <= hy.
	 * Returns true if the line is not visible.
	 */
	boolean clipLine(Point from, Point to, int lx, int hx, int ly, int hy)
	{
		for(;;)
		{
			// compute code bits for "from" point
			int fc = 0;
			if (from.x < lx) fc |= LEFT; else
				if (from.x > hx) fc |= RIGHT;
			if (from.y < ly) fc |= BOTTOM; else
				if (from.y > hy) fc |= TOP;

			// compute code bits for "to" point
			int tc = 0;
			if (to.x < lx) tc |= LEFT; else
				if (to.x > hx) tc |= RIGHT;
			if (to.y < ly) tc |= BOTTOM; else
				if (to.y > hy) tc |= TOP;

			// look for trivial acceptance or rejection
			if (fc == 0 && tc == 0) return false;
			if (fc == tc || (fc & tc) != 0) return true;

			// make sure the "from" side needs clipping
			if (fc == 0)
			{
				int t = from.x;   from.x = to.x;   to.x = t;
				t = from.y;   from.y = to.y;   to.y = t;
				t = fc;       fc = tc;         tc = t;
			}

			if ((fc&LEFT) != 0)
			{
				if (to.x == from.x) return true;
				int t = (to.y - from.y) * (lx - from.x) / (to.x - from.x);
				from.y += t;
				from.x = lx;
			}
			if ((fc&RIGHT) != 0)
			{
				if (to.x == from.x) return true;
				int t = (to.y - from.y) * (hx - from.x) / (to.x - from.x);
				from.y += t;
				from.x = hx;
			}
			if ((fc&BOTTOM) != 0)
			{
				if (to.y == from.y) return true;
				int t = (to.x - from.x) * (ly - from.y) / (to.y - from.y);
				from.x += t;
				from.y = ly;
			}
			if ((fc&TOP) != 0)
			{
				if (to.y == from.y) return true;
				int t = (to.x - from.x) * (hy - from.y) / (to.y - from.y);
				from.x += t;
				from.y = hy;
			}
		}
	}

	private Point [] clipPoly(Point [] points, int lx, int hx, int ly, int hy)
	{
		// see if any points are outside
		int count = points.length;
		int pre = 0;
		for(int i=0; i<count; i++)
		{
			if (points[i].x < lx) pre |= LEFT; else
				if (points[i].x > hx) pre |= RIGHT;
			if (points[i].y < ly) pre |= BOTTOM; else
				if (points[i].y > hy) pre |= TOP;
		}
		if (pre == 0) return points;

		// get polygon
		Point [] in = new Point[count*2];
		for(int i=0; i<count*2; i++)
		{
			in[i] = new Point();
			if (i < count) in[i].setLocation(points[i]);
		}
		Point [] out = new Point[count*2];
		for(int i=0; i<count*2; i++)
			out[i] = new Point();

		// clip on all four sides
		Point [] a = in;
		Point [] b = out;

		if ((pre & LEFT) != 0)
		{
			count = clipEdge(a, count, b, LEFT, lx);
			Point [] swap = a;   a = b;   b = swap;
		}
		if ((pre & RIGHT) != 0)
		{
			count = clipEdge(a, count, b, RIGHT, hx);
			Point [] swap = a;   a = b;   b = swap;
		}
		if ((pre & TOP) != 0)
		{
			count = clipEdge(a, count, b, TOP, hy);
			Point [] swap = a;   a = b;   b = swap;
		}
		if ((pre & BOTTOM) != 0)
		{
			count = clipEdge(a, count, b, BOTTOM, ly);
			Point [] swap = a;   a = b;   b = swap;
		}

		// remove redundant points from polygon
		pre = 0;
		for(int i=0; i<count; i++)
		{
			if (i > 0 && a[i-1].x == a[i].x && a[i-1].y == a[i].y) continue;
			b[pre].x = a[i].x;   b[pre].y = a[i].y;
			pre++;
		}

		// closed polygon: remove redundancy on wrap-around
		while (pre != 0 && b[0].x == b[pre-1].x && b[0].y == b[pre-1].y) pre--;
		count = pre;

		// copy the polygon back if it in the wrong place
		Point [] retArr = new Point[count];
		for(int i=0; i<count; i++)
			retArr[i] = b[i];
		return retArr;
	}

	/*
	 * Method to clip polygon "in" against line "edge" (1:left, 2:right,
	 * 4:bottom, 8:top) and place clipped result in "out".
	 */
	private int clipEdge(Point [] in, int inCount, Point [] out, int edge, int value)
	{
		// look at all the lines
		Point first = new Point();
		Point second = new Point();
		int firstx = 0, firsty = 0;
		int outcount = 0;
		for(int i=0; i<inCount; i++)
		{
			int pre = i - 1;
			if (i == 0) pre = inCount-1;
			first.setLocation(in[pre]);
			second.setLocation(in[i]);
			if (clipSegment(first, second, edge, value)) continue;
			int x1 = first.x;     int y1 = first.y;
			int x2 = second.x;    int y2 = second.y;
			if (outcount != 0)
			{
				if (x1 != out[outcount-1].x || y1 != out[outcount-1].y)
				{
					out[outcount].x = x1;  out[outcount++].y = y1;
				}
			} else { firstx = x1;  firsty = y1; }
			out[outcount].x = x2;  out[outcount++].y = y2;
		}
		if (outcount != 0 && (out[outcount-1].x != firstx || out[outcount-1].y != firsty))
		{
			out[outcount].x = firstx;   out[outcount++].y = firsty;
		}
		return outcount;
	}

	/*
	 * Method to do clipping on the vector from (x1,y1) to (x2,y2).
	 * If the vector is completely invisible, true is returned.
	 */
	private boolean clipSegment(Point p1, Point p2, int codebit, int value)
	{
		int x1 = p1.x;   int y1 = p1.y;
		int x2 = p2.x;   int y2 = p2.y;

		int c1 = 0, c2 = 0;
		if (codebit == LEFT)
		{
			if (x1 < value) c1 = codebit;
			if (x2 < value) c2 = codebit;
		} else if (codebit == BOTTOM)
		{
			if (y1 < value) c1 = codebit;
			if (y2 < value) c2 = codebit;
		} else if (codebit == RIGHT)
		{
			if (x1 > value) c1 = codebit;
			if (x2 > value) c2 = codebit;
		} else if (codebit == TOP)
		{
			if (y1 > value) c1 = codebit;
			if (y2 > value) c2 = codebit;
		}

		if (c1 == c2) return c1 != 0;
		boolean flip = false;
		if (c1 == 0)
		{
			int t = x1;   x1 = x2;   x2 = t;
			t = y1;   y1 = y2;   y2 = t;
			flip = true;
		}
		if (codebit == LEFT || codebit == RIGHT)
		{
			int t = (y2-y1) * (value-x1) / (x2-x1);
			y1 += t;
			x1 = value;
		} else if (codebit == BOTTOM || codebit == TOP)
		{
			int t = (x2-x1) * (value-y1) / (y2-y1);
			x1 += t;
			y1 = value;
		}
		if (flip)
		{
			p1.x = x2;   p1.y = y2;
			p2.x = x1;   p2.y = y1;
		} else
		{
			p1.x = x1;   p1.y = y1;
			p2.x = x2;   p2.y = y2;
		}
		return false;
	}

}
