/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Poly.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Rectangle;
import java.awt.Font;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * The Poly class describes an extended set of points
 * that can be outlines, filled shapes, curves, text, and more.
 * The Poly also contains a Layer and some connectivity information.
 */
public class Poly implements Shape
{
	/**
	 * Type is a typesafe enum class that describes the nature of a Poly.
	 */
	public static class Type
	{
		private String name;
		private boolean isText;

		private Type(String name, boolean isText)
		{
			this.name = name;
			this.isText = isText;
		}

		/**
		 * Method to tell whether this Poly Style is text.
		 * @return true if this Poly Style is text.
		 */
		public boolean isText() { return isText; }

		/**
		 * Returns a printable version of this Type.
		 * @return a printable version of this Type.
		 */
		public String toString() { return "Poly.Type "+name; }

		// ************************ polygons ************************
		/**
		 * Describes a closed polygon which is filled in.
		 */
		public static final Type FILLED = new Type("filled", false);
		/**
		 * Describes a closed polygon with only the outline drawn.
		 */
		public static final Type CLOSED = new Type("closed", false);
		/**
		 * Describes a closed rectangle with the outline drawn and an "X" drawn through it.
		 */
		public static final Type CROSSED = new Type("crossed", false);

		// ************************ lines ************************
		/**
		 * Describes an open outline.
		 * The last point is not implicitly connected to the first point.
		 */
		public static final Type OPENED = new Type("opened", false);
		/**
		 * Describes an open outline, drawn with a dotted texture.
		 * The last point is not implicitly connected to the first point.
		 */
		public static final Type OPENEDT1 = new Type("opened-dotted", false);
		/**
		 * Describes an open outline, drawn with a dashed texture. 
		 * The last point is not implicitly connected to the first point.
		 */
		public static final Type OPENEDT2 = new Type("opened-dashed", false);
		/**
		 * Describes an open outline, drawn with thicker lines.
		 * The last point is not implicitly connected to the first point.
		 */
		public static final Type OPENEDT3 = new Type("opened-thick", false);
		/**
		 * Describes a vector endpoint pairs, solid.
		 * There must be an even number of points in the Poly so that vectors can be drawn from point 0 to 1,
		 * then from point 2 to 3, etc.
		 */
		public static final Type VECTORS = new Type("vectors", false);

		// ************************ curves ************************
		/**
		 * Describes a circle (only the outline is drawn).
		 * The first point is the center of the circle and the second point is on the edge, thus defining the radius.
		 * This second point should be on the same horizontal level as the radius point to make radius computation easier.
		 */
		public static final Type CIRCLE = new Type("circle", false);
		/**
		 * Describes a circle, drawn with thick lines (only the outline is drawn).
		 * The first point is the center of the circle and the second point is on the edge, thus defining the radius.
		 * This second point should be on the same horizontal level as the radius point to make radius computation easier.
		 */
		public static final Type THICKCIRCLE = new Type("thick-circle", false);
		/**
		 * Describes a filled circle.
		 * The first point is the center of the circle and the second point is on the edge, thus defining the radius.
		 * This second point should be on the same horizontal level as the radius point to make radius computation easier.
		 */
		public static final Type DISC = new Type("disc", false);
		/**
		 * Describes an arc of a circle.
		 * The first point is the center of the circle, the second point is the start of the arc, and
		 * the third point is the end of the arc.
		 * The arc will be drawn counter-clockwise from the start point to the end point.
		 */
		public static final Type CIRCLEARC = new Type("circle-arc", false);
		/**
		 * Describes an arc of a circle, drawn with thick lines.
		 * The first point is the center of the circle, the second point is the start of the arc, and
		 * the third point is the end of the arc.
		 * The arc will be drawn counter-clockwise from the start point to the end point.
		 */
		public static final Type THICKCIRCLEARC = new Type("thick-circle-arc", false);

		// ************************ text ************************
		/**
		 * Describes text that should be centered about the Poly point.
		 * Only one point need be specified.
		 */
		public static final Type TEXTCENT = new Type("text-center", true);
		/**
		 * Describes text that should be placed so that the Poly point is at the top-center.
		 * Only one point need be specified, and the text will be below that point.
		 */
		public static final Type TEXTTOP = new Type("text-top", true);
		/**
		 * Describes text that should be placed so that the Poly point is at the bottom-center.
		 * Only one point need be specified, and the text will be above that point.
		 */
		public static final Type TEXTBOT = new Type("text-bottom", true);
		/**
		 * Describes text that should be placed so that the Poly point is at the left-center.
		 * Only one point need be specified, and the text will be to the right of that point.
		 */
		public static final Type TEXTLEFT = new Type("text-left", true);
		/**
		 * Describes text that should be placed so that the Poly point is at the right-center.
		 * Only one point need be specified, and the text will be to the left of that point.
		 */
		public static final Type TEXTRIGHT = new Type("text-right", true);
		/**
		 * Describes text that should be placed so that the Poly point is at the upper-left.
		 * Only one point need be specified, and the text will be to the lower-right of that point.
		 */
		public static final Type TEXTTOPLEFT = new Type("text-topleft", true);
		/**
		 * Describes text that should be placed so that the Poly point is at the lower-left.
		 * Only one point need be specified, and the text will be to the upper-right of that point.
		 * This is the normal starting point for most text.
		 */
		public static final Type TEXTBOTLEFT = new Type("text-botleft", true);
		/**
		 * Describes text that should be placed so that the Poly point is at the upper-right.
		 * Only one point need be specified, and the text will be to the lower-left of that point.
		 */
		public static final Type TEXTTOPRIGHT = new Type("text-topright", true);
		/**
		 * Describes text that should be placed so that the Poly point is at the lower-right.
		 * Only one point need be specified, and the text will be to the upper-left of that point.
		 */
		public static final Type TEXTBOTRIGHT = new Type("text-botright", true);
		/**
		 * Describes text that is centered in the Poly and must remain inside.
		 * If the letters do not fit, a smaller font will be used, and if that still does not work,
		 * any letters that cannot fit are not written.
		 * The Poly coordinates must define an area for the text to live in.
		 */
		public static final Type TEXTBOX = new Type("text-box", true);

		// ************************ miscellaneous ************************
		/**
		 * Describes a small cross, drawn at the specified location.
		 * Typically there will be only one point in this polygon
		 * but if there are more they are averaged and the cross is drawn in the center.
		 */
		public static final Type CROSS = new Type("cross", false);
		/**
		 * Describes a big cross, drawn at the specified location.
		 * Typically there will be only one point in this polygon
		 * but if there are more they are averaged and the cross is drawn in the center.
		 */
		public static final Type BIGCROSS = new Type("big-cross", false);

		/**
		 * Method to get the "angle" of a style of text.
		 * When rotating a node, the anchor point also rotates.
		 * To to this elegantly, the Type is converted to an angle, rotated, and then converted back to a Type.
		 * @return the angle of this text Type.
		 */
		public int getTextAngle()
		{
			if (this == TEXTLEFT) return 0;
			if (this == TEXTBOTLEFT) return 450;
			if (this == TEXTBOT) return 900;
			if (this == TEXTBOTRIGHT) return 1350;
			if (this == TEXTRIGHT) return 1800;
			if (this == TEXTTOPRIGHT) return 2250;
			if (this == TEXTTOP) return 2700;
			if (this == TEXTTOPLEFT) return 3150;
			return 0;
		}

		/**
		 * Method to get a text Type from an angle.
		 * When rotating a node, the anchor point also rotates.
		 * To to this elegantly, the Type is converted to an angle, rotated, and then converted back to a Type.
		 * @param the angle of the text anchor.
		 * @return a text Type that corresponds to the angle.
		 */
		public static Type getTextTypeFromAngle(int angle)
		{
			switch (angle)
			{
				case 0:    return TEXTLEFT;
				case 450:  return TEXTBOTLEFT;
				case 900:  return TEXTBOT;
				case 1350: return TEXTBOTRIGHT;
				case 1800: return TEXTRIGHT;
				case 2250: return TEXTTOPRIGHT;
				case 2700: return TEXTTOP;
				case 3150: return TEXTTOPLEFT;
			}
			return TEXTCENT;
		}
	}

	/** the style (outline, text, lines, etc.) */			private Poly.Type style;
	/** the points */										private Point2D points[];
	/** the layer (used for graphics) */					private Layer layer;
	/** the bounds of the points */							private Rectangle2D bounds;
	/** the string (if of type TEXT) */						private String string;
	/** the Name (if of type TEXT) */						private Name name;
	/** the text descriptor (if of type TEXT) */			private TextDescriptor descript;
	/** the variable (if of type TEXT) */					private Variable var;
	/** the PortProto (if from a node or TEXT) */			private PortProto pp;


	/**
	 * The constructor creates a new Poly given an array of points.
	 * @param points the array of coordinates.
	 */
	public Poly(Point2D [] points)
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
	public Poly(double cX, double cY, double width, double height)
	{
		double halfWidth = width / 2;
		double halfHeight = height / 2;
		initialize(makePoints(cX-halfWidth, cX+halfWidth, cY-halfHeight, cY+halfHeight));
	}

	/**
	 * The constructor creates a new Poly that describes a rectangle.
	 * @param rect the Rectangle2D of the rectangle.
	 */
	public Poly(Rectangle2D rect)
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
		this.style = null;
		this.points = points;
		this.layer = null;
		this.bounds = null;
		this.string = null;
		this.name = null;
		this.descript = null;
		this.var = null;
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
	 * Method to return the String associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * @return the String associated with this Poly.
	 */
	public String getString() { return string; }

	/**
	 * Method to set the String associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * @param string the String associated with this Poly.
	 */
	public void setString(String string) { this.string = string; }

	/**
	 * Method to return the Name associated with this Poly.
	 * This only applies to text Polys which come from Named objects (Node and Arc names).
	 * @return the Name associated with this Poly.
	 */
	public Name getName() { return name; }

	/**
	 * Method to set the String associated with this Poly.
	 * This only applies to text Polys which come from Named objects (Node and Arc names).
	 * @param name the Name associated with this Poly.
	 */
	public void setName(Name name) { this.name = name; }

	/**
	 * Method to return the Text Descriptor associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * Only the size, face, italic, bold, and underline fields are relevant.
	 * @return the Text Descriptor associated with this Poly.
	 */
	public TextDescriptor getTextDescriptor() { return descript; }

	/**
	 * Method to set the Text Descriptor associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * Only the size, face, italic, bold, and underline fields are relevant.
	 * @param descript the Text Descriptor associated with this Poly.
	 */
	public void setTextDescriptor(TextDescriptor descript) { this.descript = descript; }

	/**
	 * Method to return the Variable associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * @return the Variable associated with this Poly.
	 */
	public Variable getVariable() { return var; }

	/**
	 * Method to set the Variable associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * @param var the Variable associated with this Poly.
	 */
	public void setVariable(Variable var) { this.var = var; }

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
		if (style == Type.CIRCLEARC || style == Type.THICKCIRCLEARC)
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
			if (style != Type.FILLED && style != Type.CLOSED && style != Type.TEXTBOX) return null;
		} else if (points.length == 5)
		{
			if (style != Type.OPENED && style != Type.OPENEDT1 &&
				style != Type.OPENEDT2 && style != Type.OPENEDT3) return null;
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
		if (style == Type.FILLED || style == Type.CLOSED || style == Type.CROSSED || style.isText())
		{
			// check rectangular case for containment
			Rectangle2D bounds = getBox();
			if (bounds != null)
			{
                //if (EMath.pointInRect(pt, bounds)) return true;
                if (EMath.pointCloseToWithinRect(pt, bounds)) return true;
                // special case: single point, take care of double precision error
                if (bounds.getWidth() == 0 && bounds.getHeight() == 0) {
                    if (EMath.doublesClose(pt.getX(), bounds.getX()) &&
                        EMath.doublesClose(pt.getY(), bounds.getY())) return true;
                }
				return false;
			}

			// general polygon containment by summing angles to vertices
			double ang = 0;
			Point2D lastPoint = points[points.length-1];
            //if (pt.equals(lastPoint)) return true;
            if (EMath.pointsClose(pt, lastPoint)) return true;
			int lastp = EMath.figureAngle(pt, lastPoint);
			for(int i=0; i<points.length; i++)
			{
				Point2D thisPoint = points[i];
				//if (pt.equals(thisPoint)) return true;
                if (EMath.pointsClose(pt, lastPoint)) return true;
				int thisp = EMath.figureAngle(pt, thisPoint);
				int tang = lastp - thisp;
				if (tang < -1800) tang += 3600;
				if (tang > 1800) tang -= 3600;
				ang += tang;
				lastp = thisp;
			}
			if (Math.abs(ang) <= points.length) return false;
			return true;
		}

		if (style == Type.CROSS || style == Type.BIGCROSS)
		{
            if (EMath.doublesClose(getCenterX(), pt.getX()) && EMath.doublesClose(getCenterY(), pt.getY())) return true;
			//if (getCenterX() == pt.getX() && getCenterY() == pt.getY()) return true;
			return false;
		}

		if (style == Type.OPENED || style == Type.OPENEDT1 || style == Type.OPENEDT2 ||
			style == Type.OPENEDT3 || style == Type.VECTORS)
		{
			// first look for trivial inclusion by being a vertex
			//for(int i=0; i<points.length; i++)
			//	if (pt.equals(points[i])) return true;
            for(int i=0; i<points.length; i++)
                if (EMath.pointsClose(pt, points[i])) return true;

			// see if the point is on one of the edges
			if (style == Type.VECTORS)
			{
				for(int i=0; i<points.length; i += 2)
					if (EMath.isCloseToLine(points[i], points[i+1], pt)) return true;
			} else
			{
				for(int i=1; i<points.length; i++)
					if (EMath.isCloseToLine(points[i-1], points[i], pt)) return true;
			}
			return false;
		}

		if (style == Type.CIRCLE || style == Type.THICKCIRCLE || style == Type.DISC)
		{
			double dist = points[0].distance(points[1]);
			double odist = points[0].distance(pt);
			if (odist < dist) return true;
			return false;
		}

		if (style == Type.CIRCLEARC || style == Type.THICKCIRCLEARC)
		{
			// first see if the point is at the proper angle from the center of the arc
			int ang = EMath.figureAngle(points[0], pt);
			int endangle = EMath.figureAngle(points[0], points[1]);
			int startangle = EMath.figureAngle(points[0], points[2]);
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
            if (EMath.doublesClose(dist, wantdist)) return true;
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
		if (style == Type.CIRCLE || style == Type.THICKCIRCLE || style == Type.DISC)
		{
			Point2D ctr = points[0];
			double dx = Math.abs(ctr.getX() - points[1].getX());
			double dy = Math.abs(ctr.getY() - points[1].getY());
			double rad = Math.max(dx, dy);
            if (!EMath.pointCloseToWithinRect(new Point2D.Double(ctr.getX()+rad,ctr.getY()+rad), bounds)) return false;
            if (!EMath.pointCloseToWithinRect(new Point2D.Double(ctr.getX()-rad,ctr.getY()-rad), bounds)) return false;
			//if (!bounds.contains(new Point2D.Double(ctr.getX()+rad,ctr.getY()+rad))) return false;
			//if (!bounds.contains(new Point2D.Double(ctr.getX()-rad,ctr.getY()-rad))) return false;
			return true;
		}
		for(int i=0; i<points.length; i++)
		{
            if (!EMath.pointCloseToWithinRect(points[i], bounds)) return false;
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
		NodeInst ni = pi.getNodeInst();
		PortProto pp = pi.getPortProto();
		AffineTransform trans = ni.rotateOut();
		while (ni.getProto() instanceof Cell)
		{
			trans = ni.translateOut(trans);
			ni = ((Export)pp).getOriginalPort().getNodeInst();
			pp = ((Export)pp).getOriginalPort().getPortProto();
			trans = ni.rotateOut(trans);
		}

		// do not reduce port if not filled
		if (getStyle() != Type.FILLED && getStyle() != Type.CROSSED &&
			getStyle() != Type.DISC) return;

		// do not reduce port areas on polygonally defined nodes
		if (ni.getTrace() != null) return;

		// determine amount to reduce port
		double realWid = wid / 2;

		// get bounding box of port polygon
		Rectangle2D portBounds = getBox();
		if (portBounds == null)
		{
			// special case: nonrectangular port
			if (getStyle() == Type.DISC)
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
		SizeOffset so = ni.getProto().getSizeOffset();
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
	 * Method to convert text Polys to their precise bounds in a given window.
	 * @param wnd the window.
	 * @param eObj the ElectricObject on which this text resides.
	 * If that ElectricObject is a NodeInst and the node is rotated, it affects the text anchor point.
	 * @return true if the text is too small to display.
	 */
	public boolean setExactTextBounds(EditWindow wnd, ElectricObject eObj)
	{
		String theString = getString().trim();
		if (theString.length() == 0) return true;
		int numLines = 1;
		if (var != null)
		{
			numLines = var.getLength();
			if (numLines > 1)
			{
				Object [] objList = (Object [])var.getObject();
				for(int i=0; i<numLines; i++)
				{
					// empty line
					if (objList[i] == null) continue;
					String str = objList[i].toString();
					if (str.length() > theString.length()) theString = str;
				}
			}
		}

		Font font = wnd.getFont(getTextDescriptor());
		if (font == null) return true;
		Rectangle2D bounds = getBounds2D();
		double lX = bounds.getMinX();
		double hX = bounds.getMaxX();
		double lY = bounds.getMinY();
		double hY = bounds.getMaxY();
		GlyphVector gv = wnd.getGlyphs(theString, font);
		Rectangle2D glyphBounds = gv.getVisualBounds();
		Type style = getStyle();
		style = rotateType(style, eObj);
		Point2D corner = getTextCorner(wnd, font, gv, style, lX, hX, lY, hY);
		double cX = corner.getX();
		double cY = corner.getY();
		double textScale = getTextScale(wnd, gv, getStyle(), lX, hX, lY, hY);
		double width = glyphBounds.getWidth() * textScale;
		double height = font.getSize() * textScale * numLines;
		switch (getTextDescriptor().getRotation().getIndex())
		{
			case 1:		// rotate 90 counterclockwise
				double saveWidth = width;
				width = height;
				height = saveWidth;
				break;
			case 2:		// rotate 180
				width = -width;
				height = -height;
				break;
			case 3:		// rotate 90 clockwise
				double saveHeight = height;
				height = width;
				width = saveHeight;
				break;
		}
		points = new Point2D.Double[] {
			new Point2D.Double(cX, cY),
			new Point2D.Double(cX+width, cY),
			new Point2D.Double(cX+width, cY+height),
			new Point2D.Double(cX, cY+height)};
		this.bounds = null;
		return false;
	}

	/**
	 * Method to rotate a text Type according to the rotation of the object on which it resides.
	 * @param origType the original text Type.
	 * @param eObj the ElectricObject on which the text resides.
	 * @return the new text Type that accounts for the rotation.
	 */
	public static Type rotateType(Type origType, ElectricObject eObj)
	{
		NodeInst ni = null;
		if (eObj instanceof NodeInst)
		{
			ni = (NodeInst)eObj;
		} else if (eObj instanceof Export)
		{
			Export pp = (Export)eObj;
			ni = pp.getOriginalPort().getNodeInst();
		} else return origType;
		int nodeAngle = ni.getAngle();
		if (nodeAngle != 0 || ni.isMirroredAboutXAxis() || ni.isMirroredAboutYAxis())
		{
			if ((nodeAngle%900) == 0 && origType != Type.TEXTCENT && origType != Type.TEXTBOX)
			{
				int angle = origType.getTextAngle();
				if (ni.isMirroredAboutXAxis() != ni.isMirroredAboutYAxis()) nodeAngle = 3600 - nodeAngle;
				angle = (angle + nodeAngle) % 3600; 
				Type style = Type.getTextTypeFromAngle(angle);
				return style;
			}
		}
		return origType;
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
	private double getTextScale(EditWindow wnd, GlyphVector gv, Poly.Type style, double lX, double hX, double lY, double hY)
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
	 * Method to return the coordinates of the lower-left corner of text in a window.
	 * @param wnd the window with the text.
	 * @param gv the GlyphVector describing the text.
	 * @param style the anchor information for the text.
	 * @param lX the low X bound of the polygon containing the text.
	 * @param hX the high X bound of the polygon containing the text.
	 * @param lY the low Y bound of the polygon containing the text.
	 * @param hY the high Y bound of the polygon containing the text.
	 * @return the coordinates of the lower-left corner of the text.
	 */
	private Point2D getTextCorner(EditWindow wnd, Font font, GlyphVector gv, Poly.Type style, double lX, double hX, double lY, double hY)
	{
		// adjust to place text in the center
		Rectangle2D glyphBounds = gv.getVisualBounds();
		double textScale = getTextScale(wnd, gv, style, lX, hX, lY, hY);
		double textWidth = glyphBounds.getWidth();
		double textHeight = font.getSize();
		double scaledWidth = textWidth * textScale;
		double scaledHeight = textHeight * textScale;
		double offX = 0, offY = 0;
		if (style == Poly.Type.TEXTCENT || style == Poly.Type.TEXTBOX)
		{
			offX = -scaledWidth/2;
			offY = -scaledHeight/2;
		} else if (style == Poly.Type.TEXTTOP)
		{
			offX = -scaledWidth/2;
			offY = -scaledHeight;
		} else if (style == Poly.Type.TEXTBOT)
		{
			offX = -scaledWidth/2;
		} else if (style == Poly.Type.TEXTLEFT)
		{
			offY = -scaledHeight/2;
		} else if (style == Poly.Type.TEXTRIGHT)
		{
			offX = -scaledWidth;
			offY = -scaledHeight/2;
		} else if (style == Poly.Type.TEXTTOPLEFT)
		{
			offY = -scaledHeight;
		} else if (style == Poly.Type.TEXTBOTLEFT)
		{
		} else if (style == Poly.Type.TEXTTOPRIGHT)
		{
			offX = -scaledWidth;
			offY = -scaledHeight;
		} else if (style == Poly.Type.TEXTBOTRIGHT)
		{
			offX = -scaledWidth;
//		} if (style == Poly.Type.TEXTBOX)
//		{
//			offX = -(textWidth * textScale) / 2;
//			offY = -(textHeight * textScale) / 2;
		}
		int rotation = getTextDescriptor().getRotation().getIndex();
		if (rotation != 0)
		{
			double saveOffX = offX;
			switch (rotation)
			{
				case 1:
					offX = offY;
					offY = saveOffX;
					break;
				case 2:
					offX = -offX;
					offY = -offY;
					break;
				case 3:
					offX = offY;
					offY = saveOffX;
					break;
			}
		}
		double cX = (lX + hX) / 2;
		double cY = (lY + hY) / 2;
		return new Point2D.Double(cX+offX, cY+offY);
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
		Type localStyle = style;
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
		if (localStyle == Type.FILLED || localStyle == Type.CROSSED || localStyle.isText())
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
				localStyle = Type.CLOSED;
			} else
			{
				if (otherBounds.intersects(polyBounds)) return Double.MIN_VALUE;
				return otherPt.distance(polyCenter);
			}
		}

		// handle closed outline figures
		if (localStyle == Type.CLOSED)
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
					double dist = EMath.distToLine(lastPt, thisPt, otherPt);
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
		if (localStyle == Type.OPENED || localStyle == Type.OPENEDT1 ||
			localStyle == Type.OPENEDT2 || localStyle == Type.OPENEDT3)
		{
			if (otherIsPoint)
			{
				double bestDist = Double.MAX_VALUE;
				for(int i=1; i<points.length; i++)
				{
					Point2D lastPt = points[i-1];
					Point2D thisPt = points[i];

					// compute distance of close point to "otherPt"
					double dist = EMath.distToLine(lastPt, thisPt, otherPt);
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
		if (localStyle == Type.VECTORS)
		{
			if (otherIsPoint)
			{
				double bestDist = Double.MAX_VALUE;
				for(int i=0; i<points.length; i += 2)
				{
					Point2D lastPt = points[i];
					Point2D thisPt = points[i+1];

					// compute distance of close point to "otherPt"
					double dist = EMath.distToLine(lastPt, thisPt, otherPt);
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
		if (localStyle == Type.CIRCLE || localStyle == Type.THICKCIRCLE || localStyle == Type.DISC)
		{
			if (otherIsPoint)
			{
				double odist = points[0].distance(points[1]);
				double dist = points[0].distance(otherPt);
				if (localStyle == Type.DISC && dist < odist) return dist-Double.MAX_VALUE;
				return Math.abs(dist-odist);
			}
		}
		if (localStyle == Type.CIRCLEARC || localStyle == Type.THICKCIRCLEARC)
		{
			if (otherIsPoint)
			{
				// determine closest point to ends of arc
				double sdist = otherPt.distance(points[1]);
				double edist = otherPt.distance(points[2]);
				double dist = Math.min(sdist, edist);

				// see if the point is in the segment of the arc
				int pang = EMath.figureAngle(points[0], otherPt);
				int sang = EMath.figureAngle(points[0], points[1]);
				int eang = EMath.figureAngle(points[0], points[2]);
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
	 * Method to return the distance between this Poly and another.
	 * @param polyOther the other Poly to consider.
	 * @return the distance between them (returns 0 if they touch or overlap).
	 */
	public double separation(Poly polyOther)
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
		for(int i=0; i<polyOther.points.length; i++)
		{
			Point2D c = closestPoint(polyOther.points[i]);
			double pd = c.distance(polyOther.points[i]);
			if (pd <= 0) return 0;
			if (pd < minPD) minPD = pd;
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
		Type localStyle = style;
		if (localStyle == Type.FILLED || localStyle == Type.CROSSED || localStyle == Type.TEXTCENT ||
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
			if (localStyle == Type.FILLED)
			{
				if (isInside(pt)) return pt;
			}
			localStyle = Type.CLOSED;
			// FALLTHROUGH 
		}
		if (localStyle == Type.CLOSED)
		{
			// check outline of description
			double bestDist = Double.MAX_VALUE;
			Point2D bestPoint = new Point2D.Double();
			for(int i=0; i<points.length; i++)
			{
				int lastI;
				if (i == 0) lastI = points.length-1; else
					lastI = i-1;
				Point2D pc = EMath.closestPointToSegment(points[lastI], points[i], pt);
				double dist = pc.distance(pt);
				if (dist > bestDist) continue;
				bestDist = dist;
				bestPoint.setLocation(pc);
			}
			return bestPoint;
		}
		if (localStyle == Type.OPENED || localStyle == Type.OPENEDT1 ||
			localStyle == Type.OPENEDT2 || localStyle == Type.OPENEDT3)
		{
			// check outline of description
			double bestDist = Double.MAX_VALUE;
			Point2D bestPoint = new Point2D.Double();
			for(int i=1; i<points.length; i++)
			{
				Point2D pc = EMath.closestPointToSegment(points[i-1], points[i], pt);
				double dist = pc.distance(pt);
				if (dist > bestDist) continue;
				bestDist = dist;
				bestPoint.setLocation(pc);
			}
			return bestPoint;
		}
		if (localStyle == Type.VECTORS)
		{
			// check outline of description
			double bestDist = Double.MAX_VALUE;
			Point2D bestPoint = new Point2D.Double();
			for(int i=0; i<points.length; i += 2)
			{
				Point2D pc = EMath.closestPointToSegment(points[i], points[i+1], pt);
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
		return isInside(new Point2D.Double(p.getX(), p.getY()));
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
		return false;
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
		return false;
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
	public boolean intersects(Poly polyOther)
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
				if (style == Type.OPENED || style == Type.OPENEDT1 ||
					style == Type.OPENEDT2 || style == Type.OPENEDT3 ||
					style == Type.VECTORS) continue;
				p = points[count-1];
			} else
			{
				p = points[i-1];
			}
			Point2D t = points[i];
			if (style == Type.VECTORS && (i&1) != 0) i++;
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
				if (style == Type.OPENED || style == Type.OPENEDT1 ||
					style == Type.OPENEDT2 || style == Type.OPENEDT3 ||
					style == Type.VECTORS) continue;
				p2 = points[count-1];
			} else
			{
				p2 = points[i-1];
			}
			Point2D t2 = points[i];
			if (style == Type.VECTORS && (i&1) != 0) i++;

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
				int ang = EMath.figureAngle(p1, t1);
				Point2D inter = EMath.intersect(p2, 900, p1, ang);
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
				int ang = EMath.figureAngle(p1, t1);
				Point2D inter = EMath.intersect(p2, 0, p1, ang);
				if (inter == null) continue;
				if (inter.getY() != p2.getY() || inter.getX() < Math.min(p2.getX(),t2.getX()) || inter.getX() > Math.max(p2.getX(),t2.getX())) continue;
				return true;
			}

			// simple bounds check
			if (Math.min(p1.getX(),t1.getX()) > Math.max(p2.getX(),t2.getX()) || Math.max(p1.getX(),t1.getX()) < Math.min(p2.getX(),t2.getX()) ||
				Math.min(p1.getY(),t1.getY()) > Math.max(p2.getY(),t2.getY()) || Math.max(p1.getY(),t1.getY()) < Math.min(p2.getY(),t2.getY())) continue;

			// general case of line intersection
			int ang1 = EMath.figureAngle(p1, t1);
			int ang2 = EMath.figureAngle(p2, t2);
			Point2D inter = EMath.intersect(p2, ang2, p1, ang1);
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
		if (style == Type.OPENED || style == Type.OPENEDT1 || style == Type.OPENEDT2 || style == Type.OPENEDT3)
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
		if (style == Type.FILLED || style == Type.CLOSED || style == Type.CROSSED || style.isText())
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
			bounds.setRect(lX, lY, hX-lX, hY-lY);
		}
	}

	private class PolyPathIterator implements PathIterator
	{
		int idx = 0;
		AffineTransform trans;

		public PolyPathIterator(Poly p, AffineTransform at)
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
        if (layer.getFunction() != poly.getLayer().getFunction()) return (false);

	    boolean geometryCheck = polySame(poly);

	    /*
	    if (!geometryCheck && buffer != null)
	        buffer.append("Elements don't represent same geometry " + getName() + " found in " + poly.getName() + "\n");
	        */
        return (geometryCheck);
    }
}
