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
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.SizeOffset;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * The Poly class describes an extended set of points
 * that can be outlines, filled shapes, curves, text, and more.
 * The Poly also contains a Layer and some connectivity information.
 */
public class PolyBase implements Shape, PolyNodeMerge
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
		return  new Point2D.Double[] {
			new Point2D.Double(lX, lY),
			new Point2D.Double(hX, lY),
			new Point2D.Double(hX, hY),
			new Point2D.Double(lX, hY)};
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
		return new Point2D.Double[] {
			new Point2D.Double(lX, lY),
			new Point2D.Double(hX, lY),
			new Point2D.Double(hX, hY),
			new Point2D.Double(lX, hY)};
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
		// Nothing to do
		if (af.getType() == AffineTransform.TYPE_IDENTITY) return;

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
		if (points.length == 4)
		{
			// only closed polygons and text can be boxes
			if (style != Poly.Type.FILLED && style != Poly.Type.CLOSED && style != Poly.Type.TEXTBOX && style != Poly.Type.CROSSED) return null;
		} else if (points.length == 5)
		{
			if (style != Poly.Type.FILLED && style != Poly.Type.CLOSED &&
				style != Poly.Type.OPENED && style != Poly.Type.OPENEDT1 &&
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
	public boolean polySame(PolyBase polyOther)
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
            if (DBMath.areEquals(pt, lastPoint)) return true;
			int lastp = DBMath.figureAngle(pt, lastPoint);
            for (Point2D thisPoint : points)
            {
				//if (pt.equals(thisPoint)) return true;
                if (DBMath.areEquals(pt, thisPoint)) return true;
                // Checking if point is along polygon edge
                if (DBMath.isOnLine(thisPoint, lastPoint, pt))
                    return true;
				int thisp = DBMath.figureAngle(pt, thisPoint);
				int tang = lastp - thisp;
				if (tang < -1800)
                    tang += 3600;
				if (tang > 1800)
                    tang -= 3600;
				ang += tang;
				lastp = thisp;
                lastPoint = thisPoint;
			}
            ang = Math.abs(ang);
            //boolean completeCircle = ang == 0 || ang == 3600;
            boolean oldCalculation = (!(ang <= points.length));
            return (oldCalculation);
			//if (Math.abs(ang) <= points.length) return false;
			//return true;
		}

		if (style == Poly.Type.CROSS || style == Poly.Type.BIGCROSS)
		{
            if (DBMath.areEquals(getCenterX(), pt.getX()) && DBMath.areEquals(getCenterY(), pt.getY())) return true;
			return false;
		}

		if (style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 || style == Poly.Type.OPENEDT2 ||
			style == Poly.Type.OPENEDT3 || style == Poly.Type.VECTORS)
		{
			// first look for trivial inclusion by being a vertex
			//for(int i=0; i<points.length; i++)
			//	if (pt.equals(points[i])) return true;
            for (Point2D point : points)
                if (DBMath.areEquals(pt, point)) return true;

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
        for (Point2D p : points)
		{
            if (!DBMath.pointInRect(p, bounds)) return false;
			//if (!bounds.contains(points[i])) return false;
		}
		return true;
	}

	/** Method to check if point is part of the point set that defines
	 * the polygon
	 * @param point
	 * @return true if found in points set
	 */
	public boolean isPointOnCorner(Point2D point)
	{
        for (Point2D p : points)
		{
			if (DBMath.areEquals(point,p))
				return (true);
		}
		return (false);
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
//		double cx = portBounds.getCenterX();  double cy = portBounds.getCenterY();

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
                for (Point2D point : points)
				{
					double x = point.getX();
					if (x < lx)x = lx;
					if (x > hx) x = hx;
					point.setLocation(x, point.getY());
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
                for (Point2D point : points)
				{
					double y = point.getY();
					if (y < ly) y = ly;
					if (y > hy) y = hy;
					point.setLocation(point.getX(), y);
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
		NodeInst ni;
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

		// determine angle of original style
		int origAngle = origType.getTextAngle();
		if (ni.isMirroredAboutXAxis() != ni.isMirroredAboutYAxis() && ((origAngle%1800) == 0 || (origAngle%1800) == 1350)) origAngle += 1800;

		// determine change in angle because of node rotation
        Orientation orient = Orientation.fromJava(nodeAngle, ni.isMirroredAboutXAxis(), ni.isMirroredAboutYAxis());
        AffineTransform trans = orient.pureRotate();
//		AffineTransform trans = NodeInst.pureRotate(nodeAngle, ni.isMirroredAboutXAxis(), ni.isMirroredAboutYAxis());
		Point2D pt = new Point2D.Double(100, 0);
		trans.transform(pt, pt);
		int xAngle = GenMath.figureAngle(new Point2D.Double(0, 0), pt);
		if (ni.isMirroredAboutXAxis() != ni.isMirroredAboutYAxis() && ((origAngle%1800) == 450)) xAngle += 900;

		// determine new angle and style
		int angle = (origAngle + xAngle) % 3600;
		Poly.Type style = Poly.Type.getTextTypeFromAngle(angle);
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
		NodeInst ni;
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
		Orientation orient = Orientation.fromJava(rotAngle, ni.isMirroredAboutXAxis(), ni.isMirroredAboutYAxis());
        AffineTransform trans = orient.pureRotate();
//		AffineTransform trans = NodeInst.pureRotate(rotAngle, ni.isMirroredAboutXAxis(), ni.isMirroredAboutYAxis());

		Point2D pt = new Point2D.Double(100, 0);
		trans.transform(pt, pt);
		int xAngle = GenMath.figureAngle(new Point2D.Double(0, 0), pt);
		if (ni.isMirroredAboutXAxis() != ni.isMirroredAboutYAxis() &&
			((angle%1800) == 0 || (angle%1800) == 1350)) angle += 1800;
		angle = (angle - xAngle + 3600) % 3600;
		return Poly.Type.getTextTypeFromAngle(angle);
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
	protected double getTextScale(EditWindow0 wnd, GlyphVector gv, Poly.Type style, double lX, double hX, double lY, double hY)
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
	 * Method to report the distance of a point to this Poly.
	 * @param x coordinate of a point.
	 * @param y coordinate of a point.
	 * @return the distance of the point to the Poly.
	 * The method returns a negative amount if the point is a direct hit on or inside
	 * the polygon (the more negative, the closer to the center).
	 */
	public double polyDistance(double x, double y)
	{
		return polyDistance(new Rectangle2D.Double(x, y, 0, 0));
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
				if (GenMath.rectsIntersect(otherBounds,polyBounds)) return Double.MIN_VALUE;
//				if (otherBounds.intersects(polyBounds)) return Double.MIN_VALUE;
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
				if (GenMath.rectsIntersect(otherBounds,polyBounds)) return Double.MIN_VALUE;
				return otherPt.distance(polyCenter);
			}
		}

		// handle circular objects
		if (localStyle == Poly.Type.CIRCLE || localStyle == Poly.Type.THICKCIRCLE || localStyle == Poly.Type.DISC)
		{
			double odist = points[0].distance(points[1]);
			double dist = points[0].distance(otherPt);
			if (otherIsPoint)
			{
				if (localStyle == Poly.Type.DISC && dist < odist) return dist-Double.MAX_VALUE;
				return Math.abs(dist-odist);
			} else
			{
				if (points[0].getX() + dist < otherBounds.getMinX()) return dist;
				if (points[0].getX() - dist > otherBounds.getMaxX()) return dist;
				if (points[0].getY() + dist < otherBounds.getMinY()) return dist;
				if (points[0].getY() - dist > otherBounds.getMaxY()) return dist;
				return Double.MIN_VALUE;
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
			} else
			{
				Point2D [] savePoints = points;
				clipArc(otherBounds.getMinX(), otherBounds.getMaxX(), otherBounds.getMinY(), otherBounds.getMaxY());
				Point2D [] newPoints = points;
				points = savePoints;
				if (newPoints.length > 0) return Double.MIN_VALUE;
				double dist = points[0].distance(points[1]);
				return points[0].distance(otherPt) - dist;
			}
		}

		// can't figure out others: use distance to polygon center
		return otherPt.distance(polyCenter);
	}

    /**
     * Method to calculate fast distance between two manhattan
     * polygons that do not intersect
     * @param polyOther the other polygon being examined with this.
     * @return non-negative distance if both polygons are manhattan types,
     * -1 if at least one of them is not manhattan.
     */
    public double separationBox(PolyBase polyOther)
    {
        Rectangle2D thisBounds = getBox();
		Rectangle2D otherBounds = polyOther.getBox();

        // Both polygons must be manhattan-shaped
        if (thisBounds == null || otherBounds == null) return -1;

        double lX1 = thisBounds.getMinX();   double hX1 = thisBounds.getMaxX();
        double lY1 = thisBounds.getMinY();   double hY1 = thisBounds.getMaxY();
        double lX2 = otherBounds.getMinX();   double hX2 = otherBounds.getMaxX();
        double lY2 = otherBounds.getMinY();   double hY2 = otherBounds.getMaxY();
        double pdx = Math.max(lX2-hX1, lX1-hX2);
        double pdy = Math.max(lY2-hY1, lY1-hY2);

        double pd = (pdx > 0 && pdy > 0) ? // Diagonal
                Math.sqrt(pdx*pdx + pdy*pdy) :
                Math.max(pdx, pdy);
        return pd;
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
        for (Point2D point : polyOther.points)
		{
			Point2D c = closestPoint(point);
			double pd = c.distance(point);
			if (pd <= 0) return 0;
			if (pd < minPD) minPD = pd;
		}

        // also compute manhattan separation and use it if better
		double minPDman = separationBox(polyOther);
        if (minPDman != -1 && minPDman < minPD) minPD = minPDman;

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
	 * @param lX the X corner of the rectangle.
	 * @param lY the Y corner of the rectangle.
	 * @param w the width of the rectangle.
	 * @param h the height of the rectangle.
	 * @return true if the rectangle is inside the Poly.
	 */
	public boolean contains(double lX, double lY, double w, double h)
	{
		// first ensure all rectangle corners are inside of the polygon
		double hX = lX + w;
		double hY = lY + h;
		if (!isInside(new Point2D.Double(lX, lY)) ||
			!isInside(new Point2D.Double(hX, lY)) ||
			!isInside(new Point2D.Double(lX, hY)) ||
			!isInside(new Point2D.Double(hX, hY))) return false;

		// because nonconvex polygons may pierce rectangle, check all edges
		for(int i=0; i<points.length; i++)
		{
			// get a polygon edge
			int last = i-1;
			if (last < 0) last = points.length-1;
			Point2D thisPt = new Point2D.Double(points[i].getX(), points[i].getY());
			Point2D lastPt = new Point2D.Double(points[last].getX(), points[last].getY());

			// if the edge is completely outside of the rectangle, it is OK
			boolean invisible = GenMath.clipLine(lastPt, thisPt, lX, hX, lY, hY);
		    if (invisible) continue;

		    // if the polygon edge line was not completely clipped, it must sit on the rectangle edge
		    if (lastPt.getX() == thisPt.getX())
		    {
		    	if (lastPt.getY() == thisPt.getY())
		    	{
		    		if (thisPt.getX() <= lX || thisPt.getX() >= hX ||
				    	thisPt.getY() <= lY || thisPt.getY() >= hY) continue;
		    	} else
		    	{
		    		if (thisPt.getX() <= lX || thisPt.getX() >= hX) continue;
		    	}
		    }
		    if (lastPt.getY() == thisPt.getY())
		    {
		    	if (thisPt.getY() <= lY || thisPt.getY() >= hY) continue;
		    }

		    // polygon edge is inside of rectangle: no containment
		    return false;
		}

		// all edges pass: rectangle is contained
		return true;
	}

	/**
	 * Method to tell whether a rectangle is inside of this Poly.
	 * This method is a requirement of the Shape implementation.
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
			Point2D p;
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
			Point2D p2;
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
     * Method to compute longest edge.
     * @return the longest edge in this PolyBase.
     */
    public double getMaxLength()
    {
        double max = 0;
		int start = 0;
		if (style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 || style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3)
			start = 1;
		for(int i=start; i<points.length; i++)
		{
			int j = i - 1;
			if (j < 0) j = points.length - 1;
            double distance = points[i].distance(points[j]);
            if (max < distance)
                max = distance;
		}
        return max;
    }

	/**
	 * Method to compute the area of this Poly.
	 * @return the area of this Poly. Return always a positive number
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
//				double sign = 0;
//				if (points[0].getX() == points[1].getX())
//				{
//					/* first line is vertical */
//					sign = (points[2].getX() - points[1].getX()) * (points[1].getY() - points[0].getY());
//				} else
//				{
//					/* first line is horizontal */
//					sign = (points[1].getX() - points[0].getX()) * (points[1].getY() - points[2].getY());
//				}
				//if (sign < 0) area = -area;
				return Math.abs(area);
			}

			return GenMath.getAreaOfPoints(points);
		}
		return 0;
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
     * Method to return the center of the bounding box containing this PolyBase
     * @return EPoint representing the center of the PolyBase bounding box.
     */
    public EPoint getCenter()
    {
        Rectangle2D b = getBounds2D();
        return new EPoint(b.getCenterX(), b.getCenterY());
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
	 * @return the bounds of this Poly.
     * @deprecated this is only implemented because Poly extends Shape. You should
     * be using getBounds2D() instead.
	 */
	public Rectangle getBounds()
	{
		if (bounds == null) calcBounds();
		Rectangle2D r = getBounds2D();
		return new Rectangle((int)r.getMinX(), (int)r.getMinY(), (int)r.getWidth(), (int)r.getHeight());
	}

	private void calcBounds()
	{
		bounds = null;
		if (style == Poly.Type.CIRCLE || style == Poly.Type.THICKCIRCLE || style == Poly.Type.DISC)
		{
			double cX = points[0].getX();
			double cY = points[0].getY();
			double radius = points[0].distance(points[1]);
			double diameter = radius * 2;
			bounds = new Rectangle2D.Double(cX - radius, cY - radius, diameter, diameter);
			return;
		}
		if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
		{
			bounds = GenMath.arcBBox(points[1], points[2], points[0]);
			return;
		}
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
			bounds = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY); // Back on Oct 1
			//bounds = new Rectangle2D.Double(DBMath.round(lX), DBMath.round(lY), DBMath.round(hX-lX), DBMath.round(hY-lY));
		}
		if (bounds == null) bounds = new Rectangle2D.Double();
	}

	/**
	 * Attempt to control rounding errors in input libraries
	 */
	public void roundPoints()
	{
		bounds = null;
        for (Point2D point : points)
		{
			point.setLocation(DBMath.round(point.getX()), DBMath.round(point.getY()));
		}
	}

    /**
     * Method to retrieve all loops that are part of this PolyBase,
     * sorted by area.
     * @return the List of loops.
     */
    public List<PolyBase> getSortedLoops()
    {
        Collection<PolyBase> set = getPointsInArea(new Area(this), layer, true, false, null);
        List<PolyBase> list = new ArrayList<PolyBase>(set);
        Collections.sort(list, AREA_COMPARATOR);
        return (list);
    }

    /**
     * Class to compare PolyBase objects
     */
    private static Comparator<PolyBase> AREA_COMPARATOR = new Comparator<PolyBase>()
    {
         /**
         * Compares PolyBase objects based on area.
         * This method doesn't guarantee (compare(x, y)==0) == (x.equals(y))
         * @param p1 first object to be compared.
         * @param p2 second object to be compared.
         * @return Returns a negative integer, zero, or a positive integer as the
         * first object has smaller than, equal to, or greater area than the second.
         * @throws ClassCastException if the arguments' types are not PolyBase.
         */
        public int compare(PolyBase p1, PolyBase p2)
		{
			double diff = p1.getArea() - p2.getArea();
			if (diff < 0.0) return -1;
			if (diff > 0.0) return 1;
			return 0;
		}
    };
    
    /**
     * Static method to get PolyBase elements associated with an Area.
     * @param area Java2D structure containing the geometrical information
     * @param layer the Layer to examine.
     * @param simple if true, polygons with inner loops will return in sample Poly.
     * @param includeLastPoint true to include the last point.
     * @return List of PolyBase elements.
     */
	public static List<PolyBase> getPointsInArea(Area area, Layer layer, boolean simple, boolean includeLastPoint, List<PolyBase> polyList)
	{
		if (area == null) return null;
		boolean isSingular = area.isSingular();

        // Complex algorithm to detect loops
        if (!isSingular)
            return (getPointsFromComplex(area, layer, polyList));

        if (polyList == null) polyList = new ArrayList<PolyBase>();
		double [] coords = new double[6];
		List<Point2D> pointList = new ArrayList<Point2D>();
		Point2D lastMoveTo = null;
		List<PolyBase> toDelete = new ArrayList<PolyBase>();

		// Gilda: best practice note: System.arraycopy
		for(PathIterator pIt = area.getPathIterator(null); !pIt.isDone(); )
		{
			int type = pIt.currentSegment(coords);
			if (type == PathIterator.SEG_CLOSE)
			{
				if (includeLastPoint && lastMoveTo != null) pointList.add(lastMoveTo);
				Point2D [] points = new Point2D[pointList.size()];
				int i = 0;
				for(Point2D p : pointList)
					points[i++] = p;
				PolyBase poly = new PolyBase(points);
				poly.setLayer(layer);
				poly.setStyle(Poly.Type.FILLED);
				lastMoveTo = null;
				toDelete.clear();
				if (!simple && !isSingular)
				{
                    for (PolyBase pn : polyList)
					{
						if (pn.contains(pointList.get(0)) ||
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

    // Creating a tree for finding the loops
    private static class PolyBaseTree
    {
        List<PolyBaseTree> list;
        PolyBase poly;

        PolyBaseTree(PolyBase p)
        {
            poly = p;
            list = new ArrayList<PolyBaseTree>();
        }

        void getLoops(int level, Stack<PolyBase> stack)
        {
            // Starting of a new polygon
            if (level%2== 0)
            {
                stack.push(poly);
            }
            else
            {
                PolyBase top = stack.pop();
                Point2D [] points = new Point2D[top.getPoints().length+poly.getPoints().length + 2];
                System.arraycopy(top.getPoints(), 0, points, 0, top.getPoints().length);
                // Adding the first point at the end to close the first loop
                points[top.getPoints().length] = (Point2D)top.getPoints()[0].clone();
                System.arraycopy(poly.getPoints(), 0, points, top.getPoints().length+1, poly.getPoints().length);
                points[points.length-1] = (Point2D)poly.getPoints()[0].clone();
                PolyBase p = new PolyBase(points);
                p.setLayer(poly.getLayer()); // they are supposed to belong to the same layer
                stack.push(p);
            }
            level++;
            for (PolyBaseTree t : list)
                t.getLoops(level, stack);
        }

        boolean add(PolyBaseTree t)
        {
            if (!poly.contains(t.poly.getPoints()[0]))
            {
                // Belong to another root
                return false;
            }

            if (list.size() == 0)
            {
                double a = poly.getArea();
                double b = t.poly.getArea();
                list.add(t);
                if (a < b)
                {
                    System.out.println("Should this happen");
                    PolyBase c = t.poly;
                    t.poly = poly;
                    poly = c;
                }
            }
            else
            {
                for (PolyBaseTree b : list)
                {
                    PolyBase pn = b.poly;
                    if (pn.contains(t.poly.getPoints()[0]))
                    {
                        return (b.add(t));
                    }
                    else if (t.poly.contains(pn.getPoints()[0]))
                        System.out.println(" Bad happen");
                }
                list.add(t);
            }
            return true;
        }
    }

    // This assumes the algorithm starts with external loop
    public static List<PolyBase> getPointsFromComplex(Area area, Layer layer, List<PolyBase> polyList)
	{
		if (area == null) return null;

        if (polyList == null) polyList = new ArrayList<PolyBase>();
		double [] coords = new double[6];
		List<Point2D> pointList = new ArrayList<Point2D>();
        List<PolyBaseTree> roots = new ArrayList<PolyBaseTree>();
        List<PolyBase> list = new ArrayList<PolyBase>();

        // Gilda: best practice note: System.arraycopy
		for(PathIterator pIt = area.getPathIterator(null); !pIt.isDone(); )
		{
			int type = pIt.currentSegment(coords);
			if (type == PathIterator.SEG_CLOSE)
			{
				Point2D [] points = new Point2D[pointList.size()];
				int i = 0;
				for(Point2D p : pointList)
					points[i++] = p;
				PolyBase poly = new PolyBase(points);
				poly.setLayer(layer);
				poly.setStyle(Poly.Type.FILLED);

                list.add(poly);

				pointList.clear();
			} else if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO)
			{
				Point2D pt = new Point2D.Double(coords[0], coords[1]);
				pointList.add(pt);
			}
			pIt.next();
		}

        Collections.sort(list, AREA_COMPARATOR);

        // areas are sorted from min to max
        // Build the hierarchy with loops
        for (int i = list.size()-1; i > -1; i--)
        {
            PolyBaseTree t = new PolyBaseTree(list.get(i));

            // Check all possible roots
            boolean added = false;
            for (PolyBaseTree r : roots)
            {
                if (r.add(t))
                {
                    added = true;
                    break;
                }
            }
            if (!added)
                roots.add(t);
        }
        // get loops from all tree roots. Even loops start a new poly
        for (PolyBaseTree r : roots)
        {
            int count = 0;
            Stack<PolyBase> s = new Stack<PolyBase>();
            r.getLoops(count, s);
            polyList.addAll(s);
        }
        return polyList;
	}

    private class PolyPathIterator implements PathIterator
	{
		int idx = 0;
		AffineTransform trans;

		public PolyPathIterator(AffineTransform at)
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
		return new PolyPathIterator(at);
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
     * Initiative CrossLibCopy.
     * It should be equals
     */
    public boolean compare(Object obj, StringBuffer buffer)
    {
        if (this == obj) return (true);

        if (obj == null || getClass() != obj.getClass())
            return (false);

        Poly poly = (Poly)obj;
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
	public static int cropBoxComplete(Rectangle2D bounds, Rectangle2D PUBox)
	{
		// if the two boxes don't touch, just return
		double bX = PUBox.getMinX();    double uX = PUBox.getMaxX();
		double bY = PUBox.getMinY();    double uY = PUBox.getMaxY();
		double lX = bounds.getMinX();   double hX = bounds.getMaxX();
		double lY = bounds.getMinY();   double hY = bounds.getMaxY();
        if (!DBMath.isGreaterThan(hX, bX) || !DBMath.isGreaterThan(hY, bY) ||
		    !DBMath.isGreaterThan(uX, lX) || !DBMath.isGreaterThan(uY, lY)) return -2;

		// if the box to be cropped is within the other, say so
        boolean blX = !DBMath.isGreaterThan(bX, lX);
		boolean uhX = !DBMath.isGreaterThan(hX, uX);
		boolean blY = !DBMath.isGreaterThan(bY, lY);
		boolean uhY = !DBMath.isGreaterThan(hY, uY);
		if (blX && uhX && blY && uhY) return 1;

		// Crop in both directions if possible, self-contained case
		// covered already
		if (bX <= lX) lX = uX;
		if (bY >= lY) hY = bY;
		if (uY <= hY) lY = hY;
		if (hX <= uX) hX = bX;
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

		// if the two boxes don't touch, just return
        if (!DBMath.isGreaterThan(hX, bX) || !DBMath.isGreaterThan(hY, bY) ||
		    !DBMath.isGreaterThan(uX, lX) || !DBMath.isGreaterThan(uY, lY)) return 0;

		// if the box to be cropped is within the other, figure out which half to remove
        boolean blX = !DBMath.isGreaterThan(bX, lX);
		boolean uhX = !DBMath.isGreaterThan(hX, uX);
		boolean blY = !DBMath.isGreaterThan(bY, lY);
		boolean uhY = !DBMath.isGreaterThan(hY, uY);

		if (blX && uhX && blY && uhY)
		{
			double lxe = lX - bX;   double hxe = uX - hX;
			double lye = lY - bY;   double hye = uY - hY;
			double biggestExt = Math.max(Math.max(lxe, hxe), Math.max(lye, hye));
            if (DBMath.areEquals(biggestExt, 0)) return 1;
            if (DBMath.areEquals(lxe, biggestExt))
			{
				lX = (lX + uX) / 2;
				if (!DBMath.isGreaterThan(hX, lX)) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
            if (DBMath.areEquals(hxe, biggestExt))
			{
				hX = (hX + bX) / 2;
				if (!DBMath.isGreaterThan(hX, lX)) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}

			if (DBMath.areEquals(lye, biggestExt))
			{
				lY = (lY + uY) / 2;
				if (!DBMath.isGreaterThan(hY, lY)) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
			if (DBMath.areEquals(hye, biggestExt))
			{
				hY = (hY + bY) / 2;
				if (!DBMath.isGreaterThan(hY, lY)) return 1;
				bounds.setRect(lX, lY, hX-lX, hY-lY);
				return 0;
			}
		}

		// reduce (lx-hx,lY-hy) bY (bX-uX,bY-uY)
		boolean crops = false;
		if (blX && uhX)
		{
			// it covers in X...crop in Y
            if (!DBMath.isGreaterThan(hY, uY)) hY = (hY + bY) / 2;
			if (blY) lY = (lY + uY) / 2;
			bounds.setRect(lX, lY, hX-lX, hY-lY);
			crops = true;
		}
        if (blY && uhY)
		{
			// it covers in Y...crop in X
            if (!DBMath.isGreaterThan(hX, uX)) hX = (hX + bX) / 2;
			if (blX) lX = (lX + uX) / 2;
			bounds.setRect(lX, lY, hX-lX, hY-lY);
			crops = true;
		}
		if (!crops) return -1;
		return 0;
	}

	private static class AngleList
	{
		private double angle;
		private double x, y;
		AngleList(Point2D pt) { x = pt.getX();   y = pt.getY(); }
	}
		

    /**
     * Method to clip a curved polygon (CIRCLE, THICKCIRCLE, DISC, CIRCLEARC, or THICKCIRCLEARC)
     * against the rectangle lx <= X <= hx and ly <= Y <= hy.
     * Adjusts the polygon to contain the visible portions.
     */
    public void clipArc(double lx, double hx, double ly, double hy)
    {
    	double plx = bounds.getMinX();
    	double phx = bounds.getMaxX();
    	double ply = bounds.getMinY();
    	double phy = bounds.getMaxY();

    	// if not clipped, stop now
    	if (plx >= lx && phx <= hx && ply >= ly && phy <= hy) return;

    	// if totally invisible, blank the polygon
    	if (plx > hx || phx < lx || ply > hy || phy < ly)
    	{
    		points = new Point2D[0];
    		return;
    	}

    	// initialize list of relevant points
    	double xc = points[0].getX();   double yc = points[0].getY();
    	double xp = points[1].getX();   double yp = points[1].getY();
    	List<AngleList> curveList = new ArrayList<AngleList>();
     	if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
    	{
    		// include arc endpoints
    		AngleList al1 = new AngleList(points[1]);
    		double dx = xp - xc;      double dy = yp - yc;
    		if (dx == 0.0 && dy == 0.0)
    		{
    			System.out.println("Domain error doing circle/circle tangents");
    			points = new Point2D[0];
    			return;
    		}
    		al1.angle = Math.atan2(dy, dx);
    		curveList.add(al1);

    		AngleList al2 = new AngleList(points[2]);
    		dx = al1.x-xc;
    		dy = al1.y-yc;
    		if (dx == 0.0 && dy == 0.0)
    		{
    			System.out.println("Domain error doing circle/circle tangents");
    			points = new Point2D[0];
    			return;
    		}
    		al2.angle = Math.atan2(dy, dx);
    		curveList.add(al2);
    	}
       	int initialCount = curveList.size();

    	// find intersection points along left edge
    	Point2D i1 = new Point2D.Double(lx, ly);
    	Point2D i2 = new Point2D.Double(lx, hy);
    	int ints = circlelineintersection(points[0], points[1], i1, i2, 0);
    	if (ints > 0) curveList.add(new AngleList(i1));
    	if (ints > 1) curveList.add(new AngleList(i2));

    	// find intersection points along top edge
    	i1 = new Point2D.Double(lx, hy);
    	i2 = new Point2D.Double(hx, hy);
    	ints = circlelineintersection(points[0], points[1], i1, i2, 0);
    	if (ints > 0) curveList.add(new AngleList(i1));
    	if (ints > 1) curveList.add(new AngleList(i2));

    	// find intersection points along right edge
    	i1 = new Point2D.Double(hx, hy);
    	i2 = new Point2D.Double(hx, ly);
    	ints = circlelineintersection(points[0], points[1], i1, i2, 0);
    	if (ints > 0) curveList.add(new AngleList(i1));
    	if (ints > 1) curveList.add(new AngleList(i2));

    	// find intersection points along bottom edge
    	i1 = new Point2D.Double(hx, ly);
    	i2 = new Point2D.Double(lx, ly);
    	ints = circlelineintersection(points[0], points[1], i1, i2, 0);
    	if (ints > 0) curveList.add(new AngleList(i1));
    	if (ints > 1) curveList.add(new AngleList(i2));

    	// if there are no intersections, arc is invisible
    	if (curveList.size() == initialCount) { points = new Point2D[0];   return; }

    	// determine angle of intersection points
    	for(int i=initialCount; i<curveList.size(); i++)
    	{
    		AngleList al = curveList.get(i);
    		if (al.y == yc && al.x == xc)
    		{
    			System.out.println("Warning: instability ahead");
    			points = new Point2D[0];
    			return;
    		}
    		double dx = al.x - xc;
    		double dy = al.y - yc;
    		if (dx == 0.0 && dy == 0.0)
    		{
    			System.out.println("Domain error doing circle/circle tangents");
    			points = new Point2D[0];
    			return;
    		}
    		al.angle = Math.atan2(dy, dx);
    	}

    	// reject points not on the arc
     	if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
    	{
    		int j = 2;
       		AngleList al0 = curveList.get(0);
       		AngleList al1 = curveList.get(1);
    		for(int i=2; i<curveList.size(); i++)
    		{
        		AngleList al = curveList.get(i);
    			if (al0.angle > al1.angle)
    			{
    				if (al.angle > al0.angle ||
    					al.angle < al1.angle) continue;
    			} else
    			{
    				if (al.angle > al0.angle &&
    					al.angle < al1.angle) continue;
    			}
    			AngleList alj = curveList.get(j);
    			alj.x = al.x;
    			alj.y = al.y;
    			alj.angle = al.angle;
    			j++;
    		}
    		while (curveList.size() > j) curveList.remove(curveList.size()-1);

    		// make sure the start of the arc is the first point
       		al0 = curveList.get(0);
            for (AngleList al : curveList)
    		{
    			if (al.angle > al0.angle)
    				al.angle -= Math.PI*2.0;
    		}
    	} else
    	{
    		// make sure all angles are negative
            for (AngleList al : curveList)
    		{
    			if (al.angle > 0.0) al.angle -= Math.PI*2.0;
    		}
    	}

    	// sort by angle
		Collections.sort(curveList, new AngleListDescending());

    	// for full circles, add in starting point to complete circle
    	if (style != Poly.Type.CIRCLEARC && style != Poly.Type.THICKCIRCLEARC)
    	{
    		AngleList al0 = curveList.get(0);
    		AngleList alNew = new AngleList(new Point2D.Double(al0.x, al0.y));
    		alNew.angle = al0.angle - Math.PI*2.0;
    		curveList.add(alNew);
    	}

    	// now examine each segment and add it, if it is in the window
    	double radius = points[0].distance(points[1]);
    	List<Point2D.Double> newIn = new ArrayList<Point2D.Double>();
    	for(int i=1; i<curveList.size(); i++)
    	{
    		int prev = i-1;
       		AngleList al = curveList.get(i);
       		AngleList alP = curveList.get(prev);
    		double midAngle = (alP.angle + al.angle) / 2.0;
    		while (midAngle < -Math.PI) midAngle += Math.PI * 2.0;
    		double midx = xc + radius * Math.cos(midAngle);
    		double midy = yc + radius * Math.sin(midAngle);
    		if (midx < lx || midx > hx || midy < ly || midy > hy) continue;

    		// add this segment
    		newIn.add(new Point2D.Double(xc, yc));
    		newIn.add(new Point2D.Double(alP.x, alP.y));
    		newIn.add(new Point2D.Double(al.x, al.y));
    	}
    	points = new Point2D[newIn.size()];
    	for(int i=0; i<newIn.size(); i++)
     		points[i] = newIn.get(i);
    	if (style == Poly.Type.THICKCIRCLE) style = Poly.Type.THICKCIRCLEARC; else
    		if (style == Poly.Type.CIRCLE) style = Poly.Type.CIRCLEARC;
    }

    /**
     * Helper class for doing curve clipping.
     */
	private static class AngleListDescending implements Comparator<AngleList>
	{
		public int compare(AngleList c1, AngleList c2)
		{
			double diff = c2.angle - c1.angle;
			if (diff < 0.0) return -1;
			if (diff > 0.0) return 1;
			return 0;
		}
	}

    /**
     * Method to find the intersection points between the circle at (icx,icy) with point (isx,isy)
     * and the line from (lx1,ly1) to (lx2,ly2).  Returns the two points in (ix1,iy1) and (ix2,iy2).
     * Allows intersection tolerance of "tolerance".
     * Returns the number of intersection points (0 if none, 1 if tangent, 2 if intersecting).
     */
    private int circlelineintersection(Point2D ctr, Point2D edge, Point2D from, Point2D to, double tolerance)
    {
    	double icx = ctr.getX();
    	double icy = ctr.getY();
    	double isx = edge.getX();
    	double isy = edge.getY();
    	double lx1 = from.getX();
    	double ly1 = from.getY();
    	double lx2 = to.getX();
    	double ly2 = to.getY();

    	// construct a line that is perpendicular to the intersection line and passes
    	// through the circle center.  It meets the intersection line at (segx, segy)
    	double segx = 0, segy = 0;
    	if (ly1 == ly2)
    	{
    		segx = icx;
    		segy = ly1;
    	} else if (lx1 == lx2)
    	{
    		segx = lx1;
    		segy = icy;
    	} else
    	{
    		// compute equation of the line
    		double fx = lx1 - lx2;   double fy = ly1 - ly2;
    		double m = fy / fx;
    		double b = -lx1;   b *= m;   b += ly1;

    		// compute perpendicular to line through the point
    		double mi = -1.0/m;
    		double bi = -icx;  bi *= mi;  bi += icy;

    		// compute intersection of the lines
    		segx = (bi-b) / (m-mi);
    		segy = m * segx + b;
    	}

    	// special case when line passes through the circle center
    	if (segx == icx && segy == icy)
    	{
    		double fx = isx - icx;   double fy = isy - icy;
    		double radius = Math.sqrt(fx*fx + fy*fy);
    		fx = lx2 - lx1;   fy = ly2 - ly1;
    		if (fx == 0.0 && fy == 0.0)
    		{
    			System.out.println("Domain error doing circle/line intersection");
    			return 0;
    		}
    		double angle = Math.atan2(fy, fx);

    		from.setLocation(icx + Math.cos(angle) * radius, icy + Math.sin(angle) * radius);
    		to.setLocation(icx + Math.cos(-angle) * radius, icy + Math.sin(-angle) * radius);
    	} else
    	{
    		// construct a right triangle with the three points: (icx, icy), (segx, segy), and (ix1,iy1)
    		// the right angle is at the point (segx, segy) and the hypotenuse is the circle radius
    		// The unknown point is (ix1, iy1), the intersection of the line and the circle.
    		// To find it, determine the angle at the point (icx, icy)
    		double fx = isx - icx;   double fy = isy - icy;
    		double radius = Math.sqrt(fx*fx + fy*fy);
    		fx = segx - icx;   fy = segy - icy;
    		double adjacent = Math.sqrt(fx*fx + fy*fy);

    		// if they are within tolerance, accept
    		if (Math.abs(adjacent - radius) < tolerance)
    		{
    			from.setLocation(segx, segy);
    			return 1;
    		}

    		// if the point is outside of the circle, quit
    		if (adjacent > radius) return 0;

    		// for zero radius, use circle center
    		if (radius == 0.0)
    		{
    			from.setLocation(icx, icy);
    			to.setLocation(icx, icy);
    		} else
    		{
    			// now determine the angle from the center to the point (segx, segy) and offset that angle
    			// by "angle".  Then project it by "radius" to get the two intersection points
    			double angle = Math.acos(adjacent / radius);
    			fx = segx - icx;   fy = segy - icy;
    			if (fx == 0.0 && fy == 0.0)
    			{
    				System.out.println("Domain error doing line/circle intersection");
    				return 0;
    			}
    			double intangle = Math.atan2(fy, fx);
    			double a1 = intangle - angle;   double a2 = intangle + angle;
    			from.setLocation(icx + Math.cos(a1) * radius, icy + Math.sin(a1) * radius);
    			to.setLocation(icx + Math.cos(a2) * radius, icy + Math.sin(a2) * radius);
    		}
    	}

    	if (from.getX() == to.getX() && from.getY() == to.getY()) return 1;
    	return 2;
    }

    //------------------------------- PolyMerge Interface -------------------------------

	/**
	 * Method to satisfy the PolyMerge interface by return the polygon (this object).
	 * @return this object.
	 */
    public PolyBase getPolygon() {return this;}
}
