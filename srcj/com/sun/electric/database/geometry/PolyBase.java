/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PolyBase.java
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
package com.sun.electric.database.geometry;

import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * The Poly class describes an extended set of points
 * that can be outlines, filled shapes, curves, text, and more.
 * The Poly also contains a Layer and some connectivity information.
 */
public class PolyBase implements Shape
{

	/** the style (outline, text, lines, etc.) */			private Poly.Type style;
	/** the points */										protected Point2D points[];
	/** the layer (used for graphics) */					private Layer layer;
	/** the bounds of the points */							protected Rectangle2D bounds;
	/** the PortProto (if from a node or TEXT) */			private PortProto pp;

    /** represents X axis */                                public static final int X = 0;
    /** represents Y axis */                                public static final int Y = 1;
    /** represents Z axis */                                public static final int Z = 2;

	/**
	 * The constructor creates a new Poly given an array of points.
	 * @param points the array of coordinates.
	 */
	public PolyBase(Point2D [] points)
	{
		initialize(points);
	}

	/**
	 * The constructor creates a new Poly that describes a rectangle.
	 * @param cX the center X coordinate of the rectangle.
	 * @param cY the center Y coordinate of the rectangle.
	 * @param width the width of the rectangle.
	 * @param height the height of the rectangle.
	 */
	public PolyBase(double cX, double cY, double width, double height)
	{
		double halfWidth = width / 2;
		double halfHeight = height / 2;
		initialize(makePoints(cX-halfWidth, cX+halfWidth,cY-halfHeight , cY+halfHeight));
	}

	/**
	 * The constructor creates a new Poly that describes a rectangle.
	 * @param rect the Rectangle2D of the rectangle.
	 */
	public PolyBase(Rectangle2D rect)
	{
		initialize(makePoints(rect));
	}

	/**
	 * Method to create an array of Points that describes a Rectangle.
	 * @param lX the low X coordinate of the rectangle.
	 * @param hX the high X coordinate of the rectangle.
	 * @param lY the low Y coordinate of the rectangle.
	 * @param hY the high Y coordinate of the rectangle.
	 * @return an array of 4 Points that describes the Rectangle.
	 */
	public static Point2D [] makePoints(double lX, double hX, double lY, double hY)
	{
		Point2D [] points = new Point2D.Double[] {
			new Point2D.Double(lX, lY),
			new Point2D.Double(hX, lY),
			new Point2D.Double(hX, hY),
			new Point2D.Double(lX, hY)};
		return points;
	}

	/**
	 * Method to create an array of Points that describes a Rectangle.
	 * @param rect the Rectangle.
	 * @return an array of 4 Points that describes the Rectangle.
	 */
	public static Point2D [] makePoints(Rectangle2D rect)
	{
		double lX = rect.getMinX();
		double hX = rect.getMaxX();
		double lY = rect.getMinY();
		double hY = rect.getMaxY();
		Point2D [] points = new Point2D.Double[] {
			new Point2D.Double(lX, lY),
			new Point2D.Double(hX, lY),
			new Point2D.Double(hX, hY),
			new Point2D.Double(lX, hY)};
		return points;
	}

	/**
	 * Method to help initialize this Poly.
	 */
	private void initialize(Point2D [] points)
	{
		this.style = Poly.Type.CLOSED;
		this.points = points;
		this.layer = null;
		this.bounds = null;
		/*
		this.string = null;
		this.name = null;
		this.descript = null;
		this.var = null;
		*/
		this.pp = null;
	}

	/**
	 * Method to return the style associated with this Poly.
	 * The style controls how the points are interpreted (FILLED, CIRCLE, etc.)
	 * @return the style associated with this Poly.
	 */
	public Poly.Type getStyle() { return style; }

	/**
	 * Method to set the style associated with this Poly.
	 * The style controls how the points are interpreted (FILLED, CIRCLE, etc.)
	 * @param style the style associated with this Poly.
	 */
	public void setStyle(Poly.Type style) { this.style = style; }

	/**
	 * Method to return the points associated with this Poly.
	 * @return the points associated with this Poly.
	 */
	public Point2D [] getPoints() { return points; }

	/**
	 * Method to return the layer associated with this Poly.
	 * @return the layer associated with this Poly.
	 */
	public Layer getLayer() { return layer; }

	/**
	 * Method to set the layer associated with this Poly.
	 * @param layer the layer associated with this Poly.
	 */
	public void setLayer(Layer layer) { this.layer = layer; }

	/**
	 * Method to return the PortProto associated with this Poly.
	 * This applies to ports on Nodes and Exports on Cells.
	 * @return the PortProto associated with this Poly.
	 */
	public PortProto getPort() { return pp; }

	/**
	 * Method to set the PortProto associated with this Poly.
	 * This applies to ports on Nodes and Exports on Cells.
	 * @param pp the PortProto associated with this Poly.
	 */
	public void setPort(PortProto pp) { this.pp = pp; }

	/**
	 * Method to transformed the points in this Poly.
	 * @param af transformation to apply.
	 */
	public void transform(AffineTransform af)
	{
		// special case for Poly type CIRCLEARC and THICKCIRCLEARC: if transposing, reverse points
		if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
		{
			double det = af.getDeterminant();
			if (det < 0) for(int i=0; i<points.length; i += 3)
			{
				double x = points[i+1].getX();
				double y = points[i+1].getY();
				points[i+1].setLocation(points[i+2].getX(), points[i+2].getY());
				points[i+2].setLocation(x, y);
			}
		}
		af.transform(points, 0, points, 0, points.length);
//		af.transform(points, points);
		bounds = null;
	}

	/**
	 * Method to return a Rectangle that describes the orthogonal box in this Poly.
	 * @return the Rectangle that describes this Poly.
	 * If the Poly is not an orthogonal box, returns null.
	 * IT IS NOT PERMITTED TO MODIFY THE RETURNED RECTANGLE
	 * (because it is taken from the internal bounds of the Poly).
	 */
	public Rectangle2D getBox()
	{
		// closed boxes must have exactly four points
		if (points.length == 4)
		{
			// only closed polygons and text can be boxes
			if (style != Poly.Type.FILLED && style != Poly.Type.CLOSED && style != Poly.Type.TEXTBOX) return null;
		} else if (points.length == 5)
		{
			if (style != Poly.Type.OPENED && style != Poly.Type.OPENEDT1 &&
				style != Poly.Type.OPENEDT2 && style != Poly.Type.OPENEDT3) return null;
			if (points[0].getX() != points[4].getX() || points[0].getY() != points[4].getY()) return null;
		} else return null;

		// make sure the polygon is rectangular and orthogonal
		if (points[0].getX() == points[1].getX() && points[2].getX() == points[3].getX() &&
			points[0].getY() == points[3].getY() && points[1].getY() == points[2].getY())
		{
			return getBounds2D();
		}
		if (points[0].getX() == points[3].getX() && points[1].getX() == points[2].getX() &&
			points[0].getY() == points[1].getY() && points[2].getY() == points[3].getY())
		{
			return getBounds2D();
		}
		return null;
	}

	/**
	 * Method to compute the minimum size of this Polygon.
	 * Only works with manhattan geometry.
	 * @return the minimum dimension.
	 */
	public double getMinSize()
	{
		Rectangle2D box = getBox();
		if (box == null) return 0;
		return Math.min(box.getWidth(), box.getHeight());
	}

	/**
	 * Method to compute the maximum size of this Polygon.
	 * Only works with manhattan geometry.
	 * @return
	 */
	public double getMaxSize()
	{
		Rectangle2D box = getBox();
		if (box == null) return 0;
		return Math.max(box.getWidth(), box.getHeight());
	}

	/**
	 * Method to compare this Poly to another.
	 * @param polyOther the other Poly to compare.
	 * @return true if the Polys are the same.
	 */
	public boolean polySame(Poly polyOther)
	{
		// polygons must have the same number of points
		Point2D [] points = getPoints();
		Point2D [] pointsO = polyOther.getPoints();
		if (points.length != pointsO.length) return false;

		// if both are boxes, compare their extents
		Rectangle2D box = getBox();
		Rectangle2D boxO = polyOther.getBox();
		if (box != null && boxO != null)
		{
			// compare box extents
			return box.equals(boxO);
		}
		if (box != null || boxO != null) return false;

		// compare these boxes the hard way
		for(int i=0; i<points.length; i++)
			if (!points[i].equals(pointsO[i])) return false;
		return true;
	}

	/**
	 * Method to tell whether a coordinate is inside of this Poly.
	 * @param pt the point in question.
	 * @return true if the point is inside of this Poly.
	 */
	public boolean isInside(Point2D pt)
	{
		if (style == Poly.Type.FILLED || style == Poly.Type.CLOSED || style == Poly.Type.CROSSED || style.isText())
		{
			// check rectangular case for containment
			Rectangle2D bounds = getBox();
			if (bounds != null)
			{
                //if (DBMath.pointInRect(pt, bounds)) return true;
                if (DBMath.pointInRect(pt, bounds)) return true;
                // special case: single point, take care of double precision error
                if (bounds.getWidth() == 0 && bounds.getHeight() == 0) {
                    if (DBMath.areEquals(pt.getX(), bounds.getX()) &&
                        DBMath.areEquals(pt.getY(), bounds.getY())) return true;
                }
				return false;
			}

			// general polygon containment by summing angles to vertices
			double ang = 0;
			Point2D lastPoint = points[points.length-1];
            //if (pt.equals(lastPoint)) return true;
            if (DBMath.pointsClose(pt, lastPoint)) return true;
			int lastp = DBMath.figureAngle(pt, lastPoint);
			for(int i=0; i<points.length; i++)
			{
				Point2D thisPoint = points[i];
				//if (pt.equals(thisPoint)) return true;
                if (DBMath.pointsClose(pt, lastPoint)) return true;
				int thisp = DBMath.figureAngle(pt, thisPoint);
				int tang = lastp - thisp;
				if (tang < -1800) tang += 3600;
				if (tang > 1800) tang -= 3600;
				ang += tang;
				lastp = thisp;
			}
			if (Math.abs(ang) <= points.length) return false;
			return true;
		}

		if (style == Poly.Type.CROSS || style == Poly.Type.BIGCROSS)
		{
            if (DBMath.areEquals(getCenterX(), pt.getX()) && DBMath.areEquals(getCenterY(), pt.getY())) return true;
			//if (getCenterX() == pt.getX() && getCenterY() == pt.getY()) return true;
			return false;
		}

		if (style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 || style == Poly.Type.OPENEDT2 ||
			style == Poly.Type.OPENEDT3 || style == Poly.Type.VECTORS)
		{
			// first look for trivial inclusion by being a vertex
			//for(int i=0; i<points.length; i++)
			//	if (pt.equals(points[i])) return true;
            for(int i=0; i<points.length; i++)
                if (DBMath.pointsClose(pt, points[i])) return true;

			// see if the point is on one of the edges
			if (style == Poly.Type.VECTORS)
			{
				for(int i=0; i<points.length; i += 2)
					if (DBMath.isOnLine(points[i], points[i+1], pt)) return true;
			} else
			{
				for(int i=1; i<points.length; i++)
					if (DBMath.isOnLine(points[i-1], points[i], pt)) return true;
			}
			return false;
		}

		if (style == Poly.Type.CIRCLE || style == Poly.Type.THICKCIRCLE || style == Poly.Type.DISC)
		{
			double dist = points[0].distance(points[1]);
			double odist = points[0].distance(pt);
			if (odist < dist) return true;
			return false;
		}

		if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
		{
			// first see if the point is at the proper angle from the center of the arc
			int ang = DBMath.figureAngle(points[0], pt);
			int endangle = DBMath.figureAngle(points[0], points[1]);
			int startangle = DBMath.figureAngle(points[0], points[2]);
			double angrange;
			if (endangle > startangle)
			{
				if (ang < startangle || ang > endangle) return false;
				angrange = endangle - startangle;
			} else
			{
				if (ang < startangle && ang > endangle) return false;
				angrange = 3600 - startangle + endangle;
			}

			// now see if the point is the proper distance from the center of the arc
			double dist = points[0].distance(pt);
			double wantdist;
			if (ang == startangle || angrange == 0)
			{
				wantdist = points[0].distance(points[1]);
			} else if (ang == endangle)
			{
				wantdist = points[0].distance(points[2]);
			} else
			{
				double startdist = points[0].distance(points[1]);
				double enddist = points[0].distance(points[2]);
				if (enddist == startdist) wantdist = startdist; else
				{
					wantdist = startdist + (ang - startangle) / angrange *
						(enddist - startdist);
				}
			}
			//if (dist == wantdist) return true;
            if (DBMath.areEquals(dist, wantdist)) return true;
			return false;
		}

		// I give up
		return false;
	}

	/**
	 * Method to tell whether a coordinates of this Poly are inside of a Rectangle2D.
	 * @param bounds the Rectangle2D in question.
	 * @return true if this Poly is completely inside of the bounds.
	 */
	public boolean isInside(Rectangle2D bounds)
	{
		if (style == Poly.Type.CIRCLE || style == Poly.Type.THICKCIRCLE || style == Poly.Type.DISC)
		{
			Point2D ctr = points[0];
			double dx = Math.abs(ctr.getX() - points[1].getX());
			double dy = Math.abs(ctr.getY() - points[1].getY());
			double rad = Math.max(dx, dy);
            if (!DBMath.pointInRect(new Point2D.Double(ctr.getX()+rad,ctr.getY()+rad), bounds)) return false;
            if (!DBMath.pointInRect(new Point2D.Double(ctr.getX()-rad,ctr.getY()-rad), bounds)) return false;
			//if (!bounds.contains(new Point2D.Double(ctr.getX()+rad,ctr.getY()+rad))) return false;
			//if (!bounds.contains(new Point2D.Double(ctr.getX()-rad,ctr.getY()-rad))) return false;
			return true;
		}
		for(int i=0; i<points.length; i++)
		{
            if (!DBMath.pointInRect(points[i], bounds)) return false;
			//if (!bounds.contains(points[i])) return false;
		}
		return true;
	}

	/**
	 * Method to reduce this Poly by the proper amount presuming that it describes a port connected to an arc.
	 * This Poly is modified in place to reduce its size.
	 * @param pi the PortInst that describes this Poly.
	 * @param wid the width of the arc connected to this port-poly.
	 * This should be the offset width, not the actual width stored in memory.
	 * @param angle the angle of the arc connected to this port-poly.
	 * If negative, do not consider arc angle.
	 */
	public void reducePortPoly(PortInst pi, double wid, int angle)
	{
		// look down to the bottom level node/port
		PortOriginal fp = new PortOriginal(pi);
		AffineTransform trans = fp.getTransformToTop();
		NodeInst ni = fp.getBottomNodeInst();

		// do not reduce port if not filled
		if (getStyle() != Poly.Type.FILLED && getStyle() != Poly.Type.CROSSED &&
			getStyle() != Poly.Type.DISC) return;

		// do not reduce port areas on polygonally defined nodes
		if (ni.getTrace() != null) return;

		// determine amount to reduce port
		double realWid = wid / 2;

		// get bounding box of port polygon
		Rectangle2D portBounds = getBox();
		if (portBounds == null)
		{
			// special case: nonrectangular port
			if (getStyle() == Poly.Type.DISC)
			{
				// shrink discs
				double dist = points[0].distance(points[1]);
				dist = Math.max(0, dist-realWid);
				points[1].setLocation(points[0].getX() + dist, points[0].getY());
				return;
			}

			// cannot handle other forms of polygon yet
			return;
		}

		// determine the edge and center of the port polygon
		double bx = portBounds.getMinX();     double ux = portBounds.getMaxX();
		double by = portBounds.getMinY();     double uy = portBounds.getMaxY();
		double cx = portBounds.getCenterX();  double cy = portBounds.getCenterY();

		// compute the area of the nodeinst
		SizeOffset so = ni.getSizeOffset();
		Rectangle2D nodeBounds = ni.getBounds();
		Point2D lowerLeft = new Point2D.Double(nodeBounds.getMinX()+so.getLowXOffset(), nodeBounds.getMinY()+so.getLowYOffset());
		trans.transform(lowerLeft, lowerLeft);
		Point2D upperRight = new Point2D.Double(nodeBounds.getMaxX()-so.getHighXOffset(), nodeBounds.getMaxY()-so.getHighYOffset());
		trans.transform(upperRight, upperRight);
		double lx = lowerLeft.getX();   double hx = upperRight.getX();
		double ly = lowerLeft.getY();   double hy = upperRight.getY();
		if (lx > hx) { double swap = lx; lx = hx;  hx = swap; }
		if (ly > hy) { double swap = ly; ly = hy;  hy = swap; }

		// do not reduce in X if arc is horizontal
		if (angle != 0 && angle != 1800)
		{
			// determine reduced port area
			lx = Math.max(bx, lx + realWid);   hx = Math.min(ux, hx - realWid);
			if (hx < lx) hx = lx = (hx + lx) / 2;

			// only clip in X if the port area is within of the reduced node X area
			if (ux >= lx && bx <= hx)
			{
				for(int j=0; j<points.length; j++)
				{
					double x = points[j].getX();
					if (x < lx)x = lx;
					if (x > hx) x = hx;
					points[j].setLocation(x, points[j].getY());
				}
			}
		}

		// do not reduce in Y if arc is vertical
		if (angle != 900 && angle != 2700)
		{
			// determine reduced port area
			ly = Math.max(by, ly + realWid);   hy = Math.min(uy, hy - realWid);
			if (hy < ly) hy = ly = (hy + ly) / 2;

			// only clip in Y if the port area is inside of the reduced node Y area
			if (uy >= ly && by <= hy)
			{
				for(int j=0; j<points.length; j++)
				{
					double y = points[j].getY();
					if (y < ly) y = ly;
					if (y > hy) y = hy;
					points[j].setLocation(points[j].getX(), y);
				}
			}
		}
	}

	/**
	 * Method to rotate a text Type according to the rotation of the object on which it resides.
	 * @param origType the original text Type.
	 * @param eObj the ElectricObject on which the text resides.
	 * @return the new text Type that accounts for the rotation.
	 */
	public static Poly.Type rotateType(Poly.Type origType, ElectricObject eObj)
	{
		// centered text does not rotate its anchor
		if (origType == Poly.Type.TEXTCENT || origType == Poly.Type.TEXTBOX) return origType;

		// get node this sits on
		NodeInst ni = null;
		if (eObj instanceof NodeInst)
		{
			ni = (NodeInst)eObj;
		} else if (eObj instanceof Export)
		{
			Export pp = (Export)eObj;
			ni = pp.getOriginalPort().getNodeInst();
		} else return origType;

		// no need to rotate anchor if the node is not transformed
		int nodeAngle = ni.getAngle();
		if (nodeAngle == 0 && !ni.isMirroredAboutXAxis() && !ni.isMirroredAboutYAxis()) return origType;

		// can only rotate anchor when node is in a manhattan orientation
		if ((nodeAngle%900) != 0) return origType;

		// rotate the anchor
		int angle = origType.getTextAngle();
		AffineTransform trans = NodeInst.pureRotate(nodeAngle, ni.isMirroredAboutXAxis(), ni.isMirroredAboutYAxis());
		Point2D pt = new Point2D.Double(100, 0);
		trans.transform(pt, pt);
		int xAngle = GenMath.figureAngle(new Point2D.Double(0, 0), pt);
		if (ni.isMirroredAboutXAxis() != ni.isMirroredAboutYAxis() &&
			((angle%1800) == 0 || (angle%1800) == 1350)) angle += 1800;
		angle = (angle + xAngle) % 3600;

		Poly.Type style = Poly.Type.getTextTypeFromAngle(angle);
//		Type revert = unRotateType(style, eObj);
//		if (revert != origType)
//		{
//			System.out.println("Rotating "+origType.name+" on node with angle="+nodeAngle+" MX="+ni.isMirroredAboutXAxis()+" MY="+ni.isMirroredAboutYAxis()+
//				" produces type="+style.name+" but unrotation gives type="+revert.name);
//		}
		return style;
	}

	/**
	 * Method to unrotate a text Type according to the rotation of the object on which it resides.
	 * Unrotation implies converting apparent anchor information to actual stored anchor information
	 * on a transformed node.  For example, if the node is rotated, and the anchor appears to be at the
	 * bottom, then the actual anchor that is stored with the node will be different (and when transformed
	 * will appear to be at the bottom).
	 * @param origType the original text Type.
	 * @param eObj the ElectricObject on which the text resides.
	 * @return the new text Type that accounts for the rotation.
	 */
	public static Poly.Type unRotateType(Poly.Type origType, ElectricObject eObj)
	{
		// centered text does not rotate its anchor
		if (origType == Poly.Type.TEXTCENT || origType == Poly.Type.TEXTBOX) return origType;

		// get node this sits on
		NodeInst ni = null;
		if (eObj instanceof NodeInst)
		{
			ni = (NodeInst)eObj;
		} else if (eObj instanceof Export)
		{
			Export pp = (Export)eObj;
			ni = pp.getOriginalPort().getNodeInst();
		} else return origType;

		// no need to rotate anchor if the node is not transformed
		int nodeAngle = ni.getAngle();
		if (nodeAngle == 0 && !ni.isMirroredAboutXAxis() && !ni.isMirroredAboutYAxis()) return origType;

		// can only rotate anchor when node is in a manhattan orientation
		if ((nodeAngle%900) != 0) return origType;

		// rotate the anchor
		int angle = origType.getTextAngle();

		int rotAngle = ni.getAngle();
		if (ni.isMirroredAboutXAxis() != ni.isMirroredAboutYAxis()) rotAngle = -rotAngle;
		AffineTransform trans = NodeInst.pureRotate(rotAngle, ni.isMirroredAboutXAxis(), ni.isMirroredAboutYAxis());

		Point2D pt = new Point2D.Double(100, 0);
		trans.transform(pt, pt);
		int xAngle = GenMath.figureAngle(new Point2D.Double(0, 0), pt);
		if (ni.isMirroredAboutXAxis() != ni.isMirroredAboutYAxis() &&
			((angle%1800) == 0 || (angle%1800) == 1350)) angle += 1800;
		angle = (angle - xAngle + 3600) % 3600;
		Poly.Type style = Poly.Type.getTextTypeFromAngle(angle);
		return style;
	}

	/**
	 * Method to return the scaling factor between database and screen for the given text.
	 * @param wnd the window with the text.
	 * @param gv the GlyphVector describing the text.
	 * @param style the anchor information for the text.
	 * @param lX the low X bound of the polygon containing the text.
	 * @param hX the high X bound of the polygon containing the text.
	 * @param lY the low Y bound of the polygon containing the text.
	 * @param hY the high Y bound of the polygon containing the text.
	 * @return the scale of the text (from database to screen).
	 */
	protected double getTextScale(EditWindow wnd, GlyphVector gv, Poly.Type style, double lX, double hX, double lY, double hY)
	{
		double textScale = 1.0/wnd.getScale();
		if (style == Poly.Type.TEXTBOX)
		{
			Rectangle2D glyphBounds = gv.getVisualBounds();
			double textWidth = glyphBounds.getWidth() * textScale;
			if (textWidth > hX - lX)
			{
				// text too big for box: scale it down
				textScale *= (hX - lX) / textWidth;
			}
		}
		return textScale;
	}

	/**
	 * Method to report the distance of a rectangle or point to this Poly.
	 * @param otherBounds the area to test for distance to the Poly.
	 * @return the distance of the area to the Poly.
	 * The method returns a negative amount if the point/area is a direct hit on or inside
	 * the polygon (the more negative, the closer to the center).
	 */
	public double polyDistance(Rectangle2D otherBounds)
	{
		// get information about this Poly
		Rectangle2D polyBounds = getBounds2D();
		double polyCX = polyBounds.getCenterX();
		double polyCY = polyBounds.getCenterY();
		Point2D polyCenter = new Point2D.Double(polyCX, polyCY);
		Poly.Type localStyle = style;
		boolean thisIsPoint = (polyBounds.getWidth() == 0 && polyBounds.getHeight() == 0);

		// get information about the other area being tested
		boolean otherIsPoint = (otherBounds.getWidth() == 0 && otherBounds.getHeight() == 0);
		double otherCX = otherBounds.getCenterX();
		double otherCY = otherBounds.getCenterY();
		Point2D otherPt = new Point2D.Double(otherCX, otherCY);

		// handle single point polygons
		if (thisIsPoint)
		{
			if (otherIsPoint)
			{
				if (polyCX == otherCX && polyCY == otherCY) return Double.MIN_VALUE;
			} else
			{
				if (otherBounds.contains(polyCenter)) return Double.MIN_VALUE;
			}
			return otherPt.distance(polyCenter);
		}

		// handle polygons that are filled in
		if (localStyle == Poly.Type.FILLED || localStyle == Poly.Type.CROSSED || localStyle.isText())
		{
			if (otherIsPoint)
			{
				// give special returned value if point is a direct hit
				if (isInside(otherPt))
				{
					return otherPt.distance(polyCenter) - Double.MAX_VALUE;
				}

				// if polygon is a box, use M.B.R. information
				Rectangle2D box = getBox();
				if (box != null)
				{
					if (otherCX > box.getMaxX()) polyCX = otherCX - box.getMaxX(); else
						if (otherCX < box.getMinX()) polyCX = box.getMinX() - otherCX; else
							polyCX = 0;
					if (otherCY > box.getMaxY()) polyCY = otherCY - box.getMaxY(); else
						if (otherCY < box.getMinY()) polyCY = box.getMinY() - otherCY; else
							polyCY = 0;
					if (polyCX == 0 || polyCY == 0) return polyCX + polyCY;
					polyCenter.setLocation(polyCX, polyCY);
					return polyCenter.distance(new Point2D.Double(0,0));
				}

				// point is outside of irregular polygon: fall into to next case
				localStyle = Poly.Type.CLOSED;
			} else
			{
				if (otherBounds.intersects(polyBounds)) return Double.MIN_VALUE;
				return otherPt.distance(polyCenter);
			}
		}

		// handle closed outline figures
		if (localStyle == Poly.Type.CLOSED)
		{
			if (otherIsPoint)
			{
				double bestDist = Double.MAX_VALUE;
				Point2D lastPt = points[points.length-1];
				for(int i=0; i<points.length; i++)
				{
					if (i != 0) lastPt = points[i-1];
					Point2D thisPt = points[i];

					// compute distance of close point to "otherPt"
					double dist = DBMath.distToLine(lastPt, thisPt, otherPt);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			} else
			{
				if (otherBounds.intersects(polyBounds)) return Double.MIN_VALUE;
				return otherPt.distance(polyCenter);
			}
		}

		// handle opened outline figures
		if (localStyle == Poly.Type.OPENED || localStyle == Poly.Type.OPENEDT1 ||
			localStyle == Poly.Type.OPENEDT2 || localStyle == Poly.Type.OPENEDT3)
		{
			if (otherIsPoint)
			{
				double bestDist = Double.MAX_VALUE;
				for(int i=1; i<points.length; i++)
				{
					Point2D lastPt = points[i-1];
					Point2D thisPt = points[i];

					// compute distance of close point to "otherPt"
					double dist = DBMath.distToLine(lastPt, thisPt, otherPt);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			} else
			{
				if (otherBounds.intersects(polyBounds)) return Double.MIN_VALUE;
				return otherPt.distance(polyCenter);
			}
		}

		// handle outline vector lists
		if (localStyle == Poly.Type.VECTORS)
		{
			if (otherIsPoint)
			{
				double bestDist = Double.MAX_VALUE;
				for(int i=0; i<points.length; i += 2)
				{
					Point2D lastPt = points[i];
					Point2D thisPt = points[i+1];

					// compute distance of close point to "otherPt"
					double dist = DBMath.distToLine(lastPt, thisPt, otherPt);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			} else
			{
				if (otherBounds.intersects(polyBounds)) return Double.MIN_VALUE;
				return otherPt.distance(polyCenter);
			}
		}

		// handle circular objects
		if (localStyle == Poly.Type.CIRCLE || localStyle == Poly.Type.THICKCIRCLE || localStyle == Poly.Type.DISC)
		{
			if (otherIsPoint)
			{
				double odist = points[0].distance(points[1]);
				double dist = points[0].distance(otherPt);
				if (localStyle == Poly.Type.DISC && dist < odist) return dist-Double.MAX_VALUE;
				return Math.abs(dist-odist);
			}
		}
		if (localStyle == Poly.Type.CIRCLEARC || localStyle == Poly.Type.THICKCIRCLEARC)
		{
			if (otherIsPoint)
			{
				// determine closest point to ends of arc
				double sdist = otherPt.distance(points[1]);
				double edist = otherPt.distance(points[2]);
				double dist = Math.min(sdist, edist);

				// see if the point is in the segment of the arc
				int pang = DBMath.figureAngle(points[0], otherPt);
				int sang = DBMath.figureAngle(points[0], points[1]);
				int eang = DBMath.figureAngle(points[0], points[2]);
				if (eang > sang)
				{
					if (pang < eang && pang > sang) return dist;
				} else
				{
					if (pang < eang || pang > sang) return dist;
				}

				// point in arc: determine distance
				double odist = points[0].distance(points[1]);
				dist = points[0].distance(otherPt);
				return Math.abs(dist-odist);
			}
		}

		// can't figure out others: use distance to polygon center
		return otherPt.distance(polyCenter);
	}

    /**
     * Method to calculate fast distance between two manhattan
     * polygons that do not intersect
     * @param polyOther
     * @return positive distance if both polygons are manhattan types,
     * -1 if at least one of them is not.
     */
    public double separationBox(PolyBase polyOther)
    {
        Rectangle2D thisBounds = getBox();
		Rectangle2D otherBounds = polyOther.getBox();

        // Both polygons must be manhattan-shaped type
        if (thisBounds == null || otherBounds == null) return -1;

        int dir = -1;
        double lX1 = thisBounds.getMinX();   double hX1 = thisBounds.getMaxX();
        double lY1 = thisBounds.getMinY();   double hY1 = thisBounds.getMaxY();
        double lX2 = otherBounds.getMinX();   double hX2 = otherBounds.getMaxX();
        double lY2 = otherBounds.getMinY();   double hY2 = otherBounds.getMaxY();
        double [][] points1 = {{thisBounds.getMinX(), thisBounds.getMinY()},
                               {thisBounds.getMaxX(), thisBounds.getMaxY()}};
        double [][] points2 = {{otherBounds.getMinX(), otherBounds.getMinY()},
                               {otherBounds.getMaxX(), otherBounds.getMaxY()}};
        double pdx = Math.max(lX2-hX1, lX1-hX2);
        double pdy = Math.max(lY2-hY1, lY1-hY2);
        double pdx1 = Math.max(points2[0][X]-points1[1][X], points1[0][X]-points2[1][X]);
        double pdy1 = Math.max(points2[0][Y]-points1[1][Y], points1[0][Y]-points2[1][Y]);

        if (pdx1 != pdx || pdy1 != pdy)
            System.out.println("Error in separtionBox");

        double pd = (pdx > 0 && pdy > 0) ? // Diagonal
                Math.sqrt(pdx*pdx + pdy*pdy) :
                Math.max(pdx, pdy);
        return (pd);
    }

	/**
	 * Method to return the distance between this Poly and another.
	 * @param polyOther the other Poly to consider.
	 * @return the distance between them (returns 0 if they touch or overlap).
	 */
	public double separation(PolyBase polyOther)
	{
		// stop now if they touch
		if (intersects(polyOther)) return 0;

        // Case both are mahnattan shaped elements
		double minPD1 = separationBox(polyOther);
        // if (minPD != -1) return minPD;

		// look at all points on polygon 1
		double minPD = 0;
		for(int i=0; i<points.length; i++)
		{
			Point2D c = polyOther.closestPoint(points[i]);
			double pd = c.distance(points[i]);
			if (pd <= 0) return 0;
			if (i == 0) minPD = pd; else
			{
				if (pd < minPD) minPD = pd;
			}
		}

		// look at all points on polygon 2
		for(int i=0; i<polyOther.points.length; i++)
		{
			Point2D c = closestPoint(polyOther.points[i]);
			double pd = c.distance(polyOther.points[i]);
			if (pd <= 0) return 0;
			if (pd < minPD) minPD = pd;
		}
        //@TODO remove this condition PROFILING
        if (minPD1 != -1 && minPD != minPD1)
        {
            System.out.println("Error in calculation in Poly.separation");
            separationBox(polyOther);
        }
		return minPD;
	}

	/**
	 * Method to find the point on this polygon closest to a given point.
	 * @param pt the given point
	 * @return a point on this Poly that is closest.
	 */
	public Point2D closestPoint(Point2D pt)
	{
		Poly.Type localStyle = style;
		if (localStyle == Poly.Type.FILLED || localStyle == Poly.Type.CROSSED || localStyle == Poly.Type.TEXTCENT ||
			localStyle.isText())
		{
			// filled polygon: check for regularity first
			Rectangle2D bounds = getBox();
			if (bounds != null)
			{
				double x = pt.getX();   double y = pt.getY();
				if (x < bounds.getMinX()) x = bounds.getMinX();
				if (x > bounds.getMaxX()) x = bounds.getMaxX();
				if (y < bounds.getMinY()) y = bounds.getMinY();
				if (y > bounds.getMaxY()) y = bounds.getMaxY();
				return new Point2D.Double(x, y);
			}
			if (localStyle == Poly.Type.FILLED)
			{
				if (isInside(pt)) return pt;
			}
			localStyle = Poly.Type.CLOSED;
			// FALLTHROUGH 
		}
		if (localStyle == Poly.Type.CLOSED)
		{
			// check outline of description
			double bestDist = Double.MAX_VALUE;
			Point2D bestPoint = new Point2D.Double();
			for(int i=0; i<points.length; i++)
			{
				int lastI;
				if (i == 0) lastI = points.length-1; else
					lastI = i-1;
				Point2D pc = DBMath.closestPointToSegment(points[lastI], points[i], pt);
				double dist = pc.distance(pt);
				if (dist > bestDist) continue;
				bestDist = dist;
				bestPoint.setLocation(pc);
			}
			return bestPoint;
		}
		if (localStyle == Poly.Type.OPENED || localStyle == Poly.Type.OPENEDT1 ||
			localStyle == Poly.Type.OPENEDT2 || localStyle == Poly.Type.OPENEDT3)
		{
			// check outline of description
			double bestDist = Double.MAX_VALUE;
			Point2D bestPoint = new Point2D.Double();
			for(int i=1; i<points.length; i++)
			{
				Point2D pc = DBMath.closestPointToSegment(points[i-1], points[i], pt);
				double dist = pc.distance(pt);
				if (dist > bestDist) continue;
				bestDist = dist;
				bestPoint.setLocation(pc);
			}
			return bestPoint;
		}
		if (localStyle == Poly.Type.VECTORS)
		{
			// check outline of description
			double bestDist = Double.MAX_VALUE;
			Point2D bestPoint = new Point2D.Double();
			for(int i=0; i<points.length; i += 2)
			{
				Point2D pc = DBMath.closestPointToSegment(points[i], points[i+1], pt);
				double dist = pc.distance(pt);
				if (dist > bestDist) continue;
				bestDist = dist;
				bestPoint.setLocation(pc);
			}
			return bestPoint;
		}

		// presume single-point polygon and use the center
		Rectangle2D bounds = getBounds2D();
		return new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
	}

	/**
	 * Method to tell whether a point is inside of this Poly.
	 * This method is a requirement of the Shape implementation.
	 * @param x the X coordinate of the point.
	 * @param y the Y coordinate of the point.
	 * @return true if the point is inside the Poly.
	 */
	public boolean contains(double x, double y)
	{
		return isInside(new Point2D.Double(x, y));
	}

	/**
	 * Method to tell whether a point is inside of this Poly.
	 * This method is a requirement of the Shape implementation.
	 * @param p the point.
	 * @return true if the point is inside the Poly.
	 */
	public boolean contains(Point2D p)
	{
		return isInside(p);
	}

	/**
	 * Method to tell whether a rectangle is inside of this Poly.
	 * This method is a requirement of the Shape implementation.
	 * THIS METHOD HAS NOT BEEN WRITTEN YET!!!
	 * @param x the X corner of the rectangle.
	 * @param y the Y corner of the rectangle.
	 * @param w the width of the rectangle.
	 * @param h the height of the rectangle.
	 * @return true if the rectangle is inside the Poly.
	 */
	public boolean contains(double x, double y, double w, double h)
	{
		// Implementation not valid for non-convex polygons
		return (isInside(new Point2D.Double(x, y)) &&
				isInside(new Point2D.Double(x+w, y)) &&
				isInside(new Point2D.Double(x, y+h)) &&
				isInside(new Point2D.Double(x+w, y+h)));
	}

	/**
	 * Method to tell whether a rectangle is inside of this Poly.
	 * This method is a requirement of the Shape implementation.
	 * THIS METHOD HAS NOT BEEN WRITTEN YET!!!
	 * @param r the rectangle.
	 * @return true if the rectangle is inside the Poly.
	 */
	public boolean contains(Rectangle2D r)
	{
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * Method to tell whether a rectangle intersects this Poly.
	 * This method is a requirement of the Shape implementation.
	 * THIS METHOD HAS NOT BEEN WRITTEN YET!!!
	 * @param x the X corner of the rectangle.
	 * @param y the Y corner of the rectangle.
	 * @param w the width of the rectangle.
	 * @param h the height of the rectangle.
	 * @return true if the rectangle intersects the Poly.
	 */
	public boolean intersects(double x, double y, double w, double h)
	{
		throw new Error("intersects method not implemented in Poly.intersects()");
		//return false;
	}

	/**
	 * Method to tell whether a rectangle intersects this Poly.
	 * This method is a requirement of the Shape implementation.
	 * THIS METHOD HAS NOT BEEN WRITTEN YET!!!
	 * @param r the rectangle.
	 * @return true if the rectangle intersects the Poly.
	 */
	public boolean intersects(Rectangle2D r)
	{
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * Method to tell whether this Poly intersects another one.
	 * @param polyOther the other Poly to test.
	 * @return true if polygons intersect (that is, if any of their lines intersect).
	 */
	public boolean intersects(PolyBase polyOther)
	{
		// quit now if bounding boxes don't overlap
		Rectangle2D thisBounds = getBounds2D();
		Rectangle2D otherBounds = polyOther.getBounds2D();
		if (thisBounds.getMaxX() < otherBounds.getMinX() ||
			otherBounds.getMaxX() < thisBounds.getMinX() ||
			thisBounds.getMaxY() < otherBounds.getMinY() ||
			otherBounds.getMaxY() < thisBounds.getMinY()) return false;

		// check each line in this Poly
		int count = points.length;
		for(int i=0; i<count; i++)
		{
			Point2D p = null;
			if (i == 0)
			{
				if (style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 ||
					style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3 ||
					style == Poly.Type.VECTORS) continue;
				p = points[count-1];
			} else
			{
				p = points[i-1];
			}
			Point2D t = points[i];
			if (style == Poly.Type.VECTORS && (i&1) != 0) i++;
			if (p.getX() == t.getX() && p.getY() == t.getY()) continue;

			// compare this line with the other Poly
			if (Math.min(p.getX(),t.getX()) > otherBounds.getMaxX() ||
				Math.max(p.getX(),t.getX()) < otherBounds.getMinX() ||
				Math.min(p.getY(),t.getY()) > otherBounds.getMaxY() ||
				Math.max(p.getY(),t.getY()) < otherBounds.getMinY())
					continue;
			if (polyOther.lineIntersect(p, t)) return true;
		}
		return false;
	}

	/**
	 * Method to return true if the line segment from (px1,py1) to (tx1,ty1)
	 * intersects any line in polygon "poly"
	 */
	private boolean lineIntersect(Point2D p1, Point2D t1)
	{
		int count = points.length;
		for(int i=0; i<count; i++)
		{
			Point2D p2 = null;
			if (i == 0)
			{
				if (style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 ||
					style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3 ||
					style == Poly.Type.VECTORS) continue;
				p2 = points[count-1];
			} else
			{
				p2 = points[i-1];
			}
			Point2D t2 = points[i];
			if (style == Poly.Type.VECTORS && (i&1) != 0) i++;

			// simple test: if it hit one of the points, it is an intersection
			if (t2.getX() == p1.getX() && t2.getY() == p1.getY()) return true;
			if (t2.getX() == t1.getX() && t2.getY() == t1.getY()) return true;

			// ignore zero-size segments
			if (p2.getX() == t2.getX() && p2.getY() == t2.getY()) continue;

			// special case: this line is vertical
			if (p2.getX() == t2.getX())
			{
				// simple bounds check
				if (Math.min(p1.getX(),t1.getX()) > p2.getX() || Math.max(p1.getX(),t1.getX()) < p2.getX()) continue;

				if (p1.getX() == t1.getX())
				{
					if (Math.min(p1.getY(),t1.getY()) > Math.max(p2.getY(),t2.getY()) ||
						Math.max(p1.getY(),t1.getY()) < Math.min(p2.getY(),t2.getY())) continue;
					return true;
				}
				if (p1.getY() == t1.getY())
				{
					if (Math.min(p2.getY(),t2.getY()) > p1.getY() || Math.max(p2.getY(),t2.getY()) < p1.getY()) continue;
					return true;
				}
				int ang = DBMath.figureAngle(p1, t1);
				Point2D inter = DBMath.intersect(p2, 900, p1, ang);
				if (inter == null) continue;
				if (inter.getX() != p2.getX() || inter.getY() < Math.min(p2.getY(),t2.getY()) || inter.getY() > Math.max(p2.getY(),t2.getY())) continue;
				return true;
			}

			// special case: this line is horizontal
			if (p2.getY() == t2.getY())
			{
				// simple bounds check
				if (Math.min(p1.getY(),t1.getY()) > p2.getY() || Math.max(p1.getY(),t1.getY()) < p2.getY()) continue;

				if (p1.getY() == t1.getY())
				{
					if (Math.min(p1.getX(),t1.getX()) > Math.max(p2.getX(),t2.getX()) ||
						Math.max(p1.getX(),t1.getX()) < Math.min(p2.getX(),t2.getX())) continue;
					return true;
				}
				if (p1.getX() == t1.getX())
				{
					if (Math.min(p2.getX(),t2.getX()) > p1.getX() || Math.max(p2.getX(),t2.getX()) < p1.getX()) continue;
					return true;
				}
				int ang = DBMath.figureAngle(p1, t1);
				Point2D inter = DBMath.intersect(p2, 0, p1, ang);
				if (inter == null) continue;
				if (inter.getY() != p2.getY() || inter.getX() < Math.min(p2.getX(),t2.getX()) || inter.getX() > Math.max(p2.getX(),t2.getX())) continue;
				return true;
			}

			// simple bounds check
			if (Math.min(p1.getX(),t1.getX()) > Math.max(p2.getX(),t2.getX()) || Math.max(p1.getX(),t1.getX()) < Math.min(p2.getX(),t2.getX()) ||
				Math.min(p1.getY(),t1.getY()) > Math.max(p2.getY(),t2.getY()) || Math.max(p1.getY(),t1.getY()) < Math.min(p2.getY(),t2.getY())) continue;

			// general case of line intersection
			int ang1 = DBMath.figureAngle(p1, t1);
			int ang2 = DBMath.figureAngle(p2, t2);
			Point2D inter = DBMath.intersect(p2, ang2, p1, ang1);
			if (inter == null) continue;
			if (inter.getX() < Math.min(p2.getX(),t2.getX()) || inter.getX() > Math.max(p2.getX(),t2.getX()) ||
				inter.getY() < Math.min(p2.getY(),t2.getY()) || inter.getY() > Math.max(p2.getY(),t2.getY()) ||
				inter.getX() < Math.min(p1.getX(),t1.getX()) || inter.getX() > Math.max(p1.getX(),t1.getX()) ||
				inter.getY() < Math.min(p1.getY(),t1.getY()) || inter.getY() > Math.max(p1.getY(),t1.getY())) continue;
			return true;
		}
		return false;
	}

	/**
	 * Method to compute the perimeter of this Poly.
	 * @return the perimeter of this Poly.
	 */
	public double getPerimeter()
	{
		double perim = 0;
		int start = 0;
		if (style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 || style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3)
			start = 1;
		for(int i=start; i<points.length; i++)
		{
			int j = i - 1;
			if (j < 0) j = points.length - 1;
			perim += points[i].distance(points[j]);
		}
		return perim;
	}

	/**
	 * Method to compute the area of this Poly.
	 * @return the area of this Poly.
	 * The calculation may return a negative value if the polygon points are counter-clockwise.
	 */
	public double getArea()
	{
		if (style == Poly.Type.FILLED || style == Poly.Type.CLOSED || style == Poly.Type.CROSSED || style.isText())
		{
			Rectangle2D bounds = getBox();
			if (bounds != null)
			{
				double area = bounds.getWidth() * bounds.getHeight();

				/* now determine the sign of the area */
				double sign = 0;
				if (points[0].getX() == points[1].getX())
				{
					/* first line is vertical */
					sign = (points[2].getX() - points[1].getX()) * (points[1].getY() - points[0].getY());
				} else
				{
					/* first line is horizontal */
					sign = (points[1].getX() - points[0].getX()) * (points[1].getY() - points[2].getY());
				}
				if (sign < 0) area = -area;
				return area;
			}

			return getAreaOfPoints(points);
		}
		return 0;
	}

	/**
	 * Method to compute the area of a polygon defined by an array of points.
	 * @param points the array of points.
	 * @return the area of the polygon defined by these points.
	 * The calculation may return a negative value if the points are counter-clockwise.
	 */
	private static double getAreaOfPoints(Point2D [] points)
	{
		double area = 0.0;
		double x0 = points[0].getX();
		double y0 = points[0].getY();
		double y1 = 0;
		for(int i=1; i<points.length; i++)
		{
			double x1 = points[i].getX();
			y1 = points[i].getY();

			/* triangulate around the polygon */
			double p1 = x1 - x0;
			double p2 = y0 + y1;
			double partial = p1 * p2;
			area += partial / 2.0f;
			x0 = x1;
			y0 = y1;
		}
		double p1 = points[0].getX() - x0;
		double p2 = points[0].getY() + y1;
		double partial = p1 * p2;
		area += partial / 2.0f;
		return area;
	}

	/**
	 * Method to return the X center coordinate of this Poly.
	 * @return the X center coordinate of this Poly.
	 */
	public double getCenterX()
	{
		Rectangle2D b = getBounds2D();
		return b.getCenterX();
	}

	/**
	 * Method to return the Y center coordinate of this Poly.
	 * @return the Y center coordinate of this Poly.
	 */
	public double getCenterY()
	{
		Rectangle2D b = getBounds2D();
		return b.getCenterY();
	}

	/**
	 * Method to return the bounds of this Poly.
	 * @return the bounds of this Poly.
	 */
	public Rectangle2D getBounds2D()
	{
		if (bounds == null) calcBounds();
		return bounds;
	}

	/**
	 * Method to return the bounds of this Poly.
	 * Nobody really uses this, but it is necessary for the implementation of Shape.
     * @deprecated this is only implemented because Poly extends Shape. You should
     * be using getBounds2D() instead.
	 * @return the bounds of this Poly.
	 */
	public Rectangle getBounds()
	{
		if (bounds == null) calcBounds();
		Rectangle2D r = getBounds2D();
		return new Rectangle((int)r.getMinX(), (int)r.getMinY(), (int)r.getWidth(), (int)r.getHeight());
	}

	private void calcBounds()
	{
		bounds = new Rectangle.Double();
		if (points.length > 0)
		{
			double lX = points[0].getX();
			double hX = lX;
			double lY = points[0].getY();
			double hY = lY;
			for (int i = 1; i < points.length; i++)
			{
				double x = points[i].getX();
				double y = points[i].getY();
				if (x < lX) lX = x;
				if (x > hX) hX = x;
				if (y < lY) lY = y;
				if (y > hY) hY = y;
			}
			bounds.setRect(lX, lY, hX-lX, hY-lY); // Back on Oct 1
			//bounds.setRect(DBMath.round(lX), DBMath.round(lY), DBMath.round(hX-lX), DBMath.round(hY-lY));
		}
	}

	/**
	 * Attempt to control rounding errors in input libraries
	 */
	public void roundPoints()
	{
		bounds = null;
		for (int i = 0; i < points.length; i++)
		{
			Point2D point = points[i];
			point.setLocation(DBMath.round(point.getX()), DBMath.round(point.getY()));
		}
	}

    /**
     * Static method to get PolyBase elements associated to an Area
     * @param area  Java2D structure containing the geometrical information
     * @param layer
     * @param simple if true, polygons with inner loops will return in sample Poly.
     * @return List of PolyBase elements
     */
	public static List getPointsInArea(Area area, Layer layer, boolean simple)
	{
		if (area == null) return null;

		List polyList = new ArrayList();
		double [] coords = new double[6];
		List pointList = new ArrayList();
		Point2D lastMoveTo = null;
		boolean isSingular = area.isSingular();
		List toDelete = new ArrayList();

		// Gilda: best practice note: System.arraycopy
		for(PathIterator pIt = area.getPathIterator(null); !pIt.isDone(); )
		{
			int type = pIt.currentSegment(coords);
			if (type == PathIterator.SEG_CLOSE)
			{
				if (lastMoveTo != null) pointList.add(lastMoveTo);
				Point2D [] points = new Point2D[pointList.size()];
				int i = 0;
				for(Iterator it = pointList.iterator(); it.hasNext(); )
					points[i++] = (Point2D)it.next();
				PolyBase poly = new PolyBase(points);
				poly.setLayer(layer);
				poly.setStyle(Poly.Type.FILLED);
				lastMoveTo = null;
				toDelete.clear();
				if (!simple && !isSingular)
				{
					Iterator it = polyList.iterator();
					while (it.hasNext())
					{
						PolyBase pn = (PolyBase)it.next();
						if (pn.contains((Point2D)pointList.get(0)) ||
						    poly.contains(pn.getPoints()[0]))
						{
							points = pn.getPoints();
							for (i = 0; i < points.length; i++)
								pointList.add(points[i]);
							Point2D[] newPoints = new Point2D[pointList.size()];
							System.arraycopy(pointList.toArray(), 0, newPoints, 0, pointList.size());
							poly = new PolyBase(newPoints);
							toDelete.add(pn);
						}
					}
				}
				if (poly != null)
					polyList.add(poly);
				polyList.removeAll(toDelete);
				pointList.clear();
			} else if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
			{
				Point2D pt = new Point2D.Double(coords[0], coords[1]);
				pointList.add(pt);
				if (type == PathIterator.SEG_MOVETO) lastMoveTo = pt;
			}
			pIt.next();
		}
		return polyList;
	}

    private class PolyPathIterator implements PathIterator
	{
		int idx = 0;
		AffineTransform trans;

		public PolyPathIterator(PolyBase p, AffineTransform at)
		{
			this.trans = at;
		}

		public int getWindingRule()
		{
			return WIND_EVEN_ODD;
		}

		public boolean isDone()
		{
			return idx > points.length;
		}

		public void next()
		{
			idx++;
		}

		public int currentSegment(float[] coords)
		{
			if (idx >= points.length)
			{
				return SEG_CLOSE;
			}
			coords[0] = (float) points[idx].getX();
			coords[1] = (float) points[idx].getY();
			if (trans != null)
			{
				trans.transform(coords, 0, coords, 0, 1);
			}
			return (idx == 0 ? SEG_MOVETO : SEG_LINETO);
		}

		public int currentSegment(double[] coords)
		{
			if (idx >= points.length)
			{
				return SEG_CLOSE;
			}
			coords[0] = points[idx].getX();
			coords[1] = points[idx].getY();
			if (trans != null)
			{
				trans.transform(coords, 0, coords, 0, 1);
			}
			return (idx == 0 ? SEG_MOVETO : SEG_LINETO);
		}
	}

	/**
	 * Method to return a PathIterator for this Poly after a transformation.
	 * This method is a requirement of the Shape implementation.
	 * @param at the transformation to apply.
	 * @return the PathIterator.
	 */
	public PathIterator getPathIterator(AffineTransform at)
	{
		return new PolyPathIterator(this, at);
	}

	/**
	 * Method to return a PathIterator with a particular flatness for this Poly after a transformation.
	 * This method is a requirement of the Shape implementation.
	 * @param at the transformation to apply.
	 * @param flatness the required flatness.
	 * @return the PathIterator.
	 */
	public PathIterator getPathIterator(AffineTransform at, double flatness)
	{
		return getPathIterator(at);
	}

    /**
     * Initiative CrossLibCopy. It should be equals
     * @param obj
     * @return
     */
    public boolean compare(Object obj, StringBuffer buffer)
    {
        if (this == obj) return (true);

        if (obj == null || getClass() != obj.getClass())
            return (false);

        Poly poly = (Poly)obj;
        Layer layer = getLayer();
        if (getLayer() != poly.getLayer())
        {
	        // Don't put until polys are sorted by layer
	        /*
	        if (buffer != null)
		        buffer.append("Elements belong to different layers " + getLayer().getName() + " found in " + poly.getLayer().getName() + "\n");
		        */
	        return (false);
        }
        // It should be covered by previous comparison
        //if (layer.getFunction() != poly.getLayer().getFunction()) return (false);

	    boolean geometryCheck = polySame(poly);

	    /*
	    if (!geometryCheck && buffer != null)
	        buffer.append("Elements don't represent same geometry " + getName() + " found in " + poly.getName() + "\n");
	        */
        return (geometryCheck);
    }

	/**
	 * Method to crop the box in the reference parameters (lx-hx, ly-hy)
	 * against the box in (bx-ux, by-uy).  If the box is cropped into oblivion,
	 * returns 1.  If the boxes overlap but cannot be cleanly cropped,
	 * returns -1.  Otherwise the box is cropped and zero is returned
	 */
	public static int cropBox(Rectangle2D bounds, Rectangle2D PUBox)
	{
		// if the two boxes don't touch, just return
		double bX = PUBox.getMinX();    double uX = PUBox.getMaxX();
		double bY = PUBox.getMinY();    double uY = PUBox.getMaxY();
		double lX = bounds.getMinX();   double hX = bounds.getMaxX();
		double lY = bounds.getMinY();   double hY = bounds.getMaxY();

		// !DBMath.isGreaterThan(hX, bX) == bX >= hX
		if (!DBMath.isGreaterThan(hX, bX) || !DBMath.isGreaterThan(hY, bY) ||
		    !DBMath.isGreaterThan(uX, lX) || !DBMath.isGreaterThan(uY, lY)) return 0;
		//if (bX >= hX || bY >= hY || uX <= lX || uY <= lY) return 0;

		// if the box to be cropped is within the other, say so
		boolean blX = !DBMath.isGreaterThan(bX, lX);
		boolean uhX = !DBMath.isGreaterThan(hX, uX);
		boolean blY = !DBMath.isGreaterThan(bY, lY);
		boolean uhY = !DBMath.isGreaterThan(hY, uY);
		//if (bX <= lX && uX >= hX && bY <= lY && uY >= hY) return 1;
		if (blX && uhX && blY && uhY) return 1;

		// see which direction is being cropped
		double xoverlap = Math.min(hX, uX) - Math.max(lX, bX);
		double yoverlap = Math.min(hY, uY) - Math.max(lY, bY);
		if (xoverlap > yoverlap)
		{
			// one above the other: crop in Y
			if (blX && uhX)
			//if (bX <= lX && uX >= hX)
			{
				// it covers in X...do the crop
				//if (uY >= hY) hY = bY;
				if (!DBMath.isGreaterThan(hY, uY)) hY = bY;
				//if (bY <= lY) lY = uY;
				if (blY) lY = uY;
				//if (hY <= lY) return 1;
				if (!DBMath.isGreaterThan(hY, lY)) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
		} else
		{
			// one next to the other: crop in X
			if (blY && uhY)
			//if (bY <= lY && uY >= hY)
			{
				// it covers in Y...crop in X
				//if (uX >= hX) hX = bX;
				if (!DBMath.isGreaterThan(hX, uX)) hX = bX;
				//if (bX <= lX) lX = uX;
				if (blX) lX = uX;
				//if (hX <= lX) return 1;
				if (!DBMath.isGreaterThan(hX, lX)) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
		}
		return -1;
	}


	/**
	 * Method to crop the box in the reference parameters (lx-hx, ly-hy)
	 * against the box in (bx-ux, by-uy). If the box is cropped into oblivion,
	 * returns 1. If the boxes overlap but cannot be cleanly cropped,
	 * returns -1. If boxes don't overlap, returns -2.
	 * Otherwise the box is cropped and zero is returned
	 */
	public static int cropBoxComplete(Rectangle2D bounds, Rectangle2D PUBox, boolean parasitic)
	{
		// if the two boxes don't touch, just return
		double bX = PUBox.getMinX();    double uX = PUBox.getMaxX();
		double bY = PUBox.getMinY();    double uY = PUBox.getMaxY();
		double lX = bounds.getMinX();   double hX = bounds.getMaxX();
		double lY = bounds.getMinY();   double hY = bounds.getMaxY();
		//if (bX >= hX || bY >= hY || uX <= lX || uY <= lY) return -2;
        if (!DBMath.isGreaterThan(hX, bX) || !DBMath.isGreaterThan(hY, bY) ||
		    !DBMath.isGreaterThan(uX, lX) || !DBMath.isGreaterThan(uY, lY)) return -2;

		// if the box to be cropped is within the other, say so
		//if (bX <= lX && uX >= hX && bY <= lY && uY >= hY) return 1;
        boolean blX = !DBMath.isGreaterThan(bX, lX);
		boolean uhX = !DBMath.isGreaterThan(hX, uX);
		boolean blY = !DBMath.isGreaterThan(bY, lY);
		boolean uhY = !DBMath.isGreaterThan(hY, uY);
		if (blX && uhX && blY && uhY) return 1;

		// Crop in both directions if possible, self-contained case
		// covered already
        // 100% self-contained case. @TODO Check this function GVG
        //if (parasitic)
        {
            if (bX <= lX) lX = uX;
            if (bY >= lY) hY = bY;
            if (uY <= hY) lY = hY;
            if (hX <= uX) hX = bX;
        }
		bounds.setRect(lX, lY, hX-lX, hY-lY);

		return 0;
	}

	/**
	 * Method to crop the box in the reference parameters (lx-hx, ly-hy)
	 * against the box in (bx-ux, by-uy).  If the box is cropped into oblivion,
	 * returns 1.  If the boxes overlap but cannot be cleanly cropped,
	 * returns -1.  Otherwise the box is cropped and zero is returned
	 */
	public static int halfCropBox(Rectangle2D bounds, Rectangle2D limit)
	{
		double bX = limit.getMinX();    double uX = limit.getMaxX();
		double bY = limit.getMinY();    double uY = limit.getMaxY();
		double lX = bounds.getMinX();   double hX = bounds.getMaxX();
		double lY = bounds.getMinY();   double hY = bounds.getMaxY();

        //@TODO Rounding error here GVG
		// if the two boxes don't touch, just return
		//if (bX >= hX || bY >= hY || uX <= lX || uY <= lY) return 0;
        if (!DBMath.isGreaterThan(hX, bX) || !DBMath.isGreaterThan(hY, bY) ||
		    !DBMath.isGreaterThan(uX, lX) || !DBMath.isGreaterThan(uY, lY)) return 0;

		// if the box to be cropped is within the other, figure out which half to remove
        boolean blX = !DBMath.isGreaterThan(bX, lX);
		boolean uhX = !DBMath.isGreaterThan(hX, uX);
		boolean blY = !DBMath.isGreaterThan(bY, lY);
		boolean uhY = !DBMath.isGreaterThan(hY, uY);

		if (blX && uhX && blY && uhY)
        //if (bX <= lX && uX >= hX && bY <= lY && uY >= hY)
		{
			double lxe = lX - bX;   double hxe = uX - hX;
			double lye = lY - bY;   double hye = uY - hY;
			double biggestExt = Math.max(Math.max(lxe, hxe), Math.max(lye, hye));
            boolean hlX = !DBMath.isGreaterThan(hX, lX);
			//if (biggestExt == 0) return 1;
            if (DBMath.areEquals(biggestExt, 0)) return 1;
			//if (lxe == biggestExt)
            if (DBMath.areEquals(lxe, biggestExt))
			{
				lX = (lX + uX) / 2;
				if (hlX) return 1;
                //if (lX >= hX) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
			//if (hxe == biggestExt)
            if (DBMath.areEquals(hxe, biggestExt))
			{
				hX = (hX + bX) / 2;
				if (hlX) return 1;
                //if (hX <= lX) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}

            boolean hlY = !DBMath.isGreaterThan(hY, lY);
            //if (lye == biggestExt)
			if (DBMath.areEquals(lye, biggestExt))
			{
				lY = (lY + uY) / 2;
				if (hlY) return 1;
                //if (lY >= hY) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
            //if (hye == biggestExt)
			if (DBMath.areEquals(hye, biggestExt))
			{
				hY = (hY + bY) / 2;
				if (hlY) return 1;
                //if (hY <= lY) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
		}

		// reduce (lx-hx,lY-hy) bY (bX-uX,bY-uY)
		boolean crops = false;
        //if (bX <= lX && uX >= hX)
		if (blX && uhX)
		{
			// it covers in X...crop in Y
			//if (uY >= hY) hY = (hY + bY) / 2;
			//if (bY <= lY) lY = (lY + uY) / 2;
            if (!DBMath.isGreaterThan(hY, uY)) hY = (hY + bY) / 2;
			if (blY) lY = (lY + uY) / 2;
			bounds.setRect(lX, lY, hX-lX, hY-lY);
			crops = true;
		}
        if (blY && uhY)
		//if (bY <= lY && uY >= hY)
		{
			// it covers in Y...crop in X
			//if (uX >= hX) hX = (hX + bX) / 2;
			//if (bX <= lX) lX = (lX + uX) / 2;
            if (!DBMath.isGreaterThan(hX, uX)) hX = (hX + bX) / 2;
			if (blX) lX = (lX + uX) / 2;
			bounds.setRect(lX, lY, hX-lX, hY-lY);
			crops = true;
		}
		if (!crops) return -1;
		return 0;
	}
}
