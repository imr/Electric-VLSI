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

import com.sun.electric.technology.Layer;
import com.sun.electric.database.geometry.EMath;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Represents a transformable Polygon with floating-point
 * coordinates, and a specific Layer. */
public class Poly implements Shape
{
	/**
	 * Type is a typesafe enum class that describes the function of an Poly.
	 */
	public static class Type
	{
		private Type()
		{
		}

		public String toString() { return "Polygon type"; }

		// polygons ************
		/** closed polygon, filled in */					public static final Type FILLED =         new Type();
		/** closed polygon, outline  */						public static final Type CLOSED =         new Type();
		// rectangles ************
//		/** closed rectangle, filled in */					public static final Type FILLEDRECT =     new Type();
//		/** closed rectangle, outline */					public static final Type CLOSEDRECT =     new Type();
		/** closed rectangle, outline crossed */			public static final Type CROSSED =        new Type();
		// lines ************
		/** open outline, solid */							public static final Type OPENED =         new Type();
		/** open outline, dotted */							public static final Type OPENEDT1 =       new Type();
		/** open outline, dashed  */						public static final Type OPENEDT2 =       new Type();
		/** open outline, thicker */						public static final Type OPENEDT3 =       new Type();
		/** open outline pushed by 1 */						public static final Type OPENEDO1 =       new Type();
		/** vector endpoint pairs, solid */					public static final Type VECTORS =        new Type();
		// curves ************
		/** circle at [0] radius to [1] */					public static final Type CIRCLE =         new Type();
		/** thick circle at [0] radius to [1] */			public static final Type THICKCIRCLE =    new Type();
		/** filled circle */								public static final Type DISC =           new Type();
		/** arc of circle at [0] ends [1] and [2] */		public static final Type CIRCLEARC =      new Type();
		/** thick arc of circle at [0] ends [1] and [2] */	public static final Type THICKCIRCLEARC = new Type();
		// text ************
		/** text at center */								public static final Type TEXTCENT =       new Type();
		/** text below top edge */							public static final Type TEXTTOP =        new Type();
		/** text above bottom edge */						public static final Type TEXTBOT =        new Type();
		/** text to right of left edge */					public static final Type TEXTLEFT =       new Type();
		/** text to left of right edge */					public static final Type TEXTRIGHT =      new Type();
		/** text to lower-right of top-left corner */		public static final Type TEXTTOPLEFT =    new Type();
		/** text to upper-right of bottom-left corner */	public static final Type TEXTBOTLEFT =    new Type();
		/** text to lower-left of top-right corner */		public static final Type TEXTTOPRIGHT =   new Type();
		/** text to upper-left of bottom-right corner */	public static final Type TEXTBOTRIGHT =   new Type();
		/** text that fits in box (may shrink) */			public static final Type TEXTBOX =        new Type();
		// miscellaneous ************
		/** grid dots in the window */						public static final Type GRIDDOTS =       new Type();
		/** cross */										public static final Type CROSS =          new Type();
		/** big cross */									public static final Type BIGCROSS =       new Type();
	}

	/** the layer (used for graphics) */					private Layer layer;
	/** the points */										private Point2D.Double points[];
	/** the bounds of the points */							private Rectangle2D.Double bounds;
	/** the style (outline, text, lines, etc.) */			private Poly.Type style;
	/** the string (if of type TEXT) */						private String string;

	/** Create a new Poly given (x,y) points and a specific Layer */
	public Poly(Point2D.Double [] points)
	{
		this.points = points;
		layer = null;
		style = null;
		bounds = null;
	}

	/** Create a new rectangular Poly given a specific Layer */
	public Poly(double cX, double cY, double width, double height)
	{
		double halfWidth = width / 2;
		double halfHeight = height / 2;
		this.points = new Point2D.Double[] {
			new Point2D.Double(cX-halfWidth, cY-halfHeight),
			new Point2D.Double(cX+halfWidth, cY-halfHeight),
			new Point2D.Double(cX+halfWidth, cY+halfHeight),
			new Point2D.Double(cX-halfWidth, cY+halfHeight)};
		layer = null;
		style = null;
		bounds = null;
	}

	/** Get the layer associated with this polygon. */
	public Layer getLayer() { return layer; }
	/** Set the layer associated with this polygon. */
	public void setLayer(Layer layer) { this.layer = layer; }

	/** Get the style (Poly.Type) associated with this polygon. */
	public Poly.Type getStyle() { return style; }
	/** Set the style (Poly.Type) associated with this polygon. */
	public void setStyle(Poly.Type style) { this.style = style; }

	/** Get the String associated with this polygon. */
	public String getString() { return string; }
	/** Set the String associated with this polygon. */
	public void setString(String string) { this.string = string; }

	/** Get the array of points associated with this polygon. */
	public Point2D.Double [] getPoints() { return points; }

	/** Get a transformed copy of this polygon, including scale, offset,
	 * and rotation.
	 * @param af transformation to apply */
	public void transform(AffineTransform af)
	{
		af.transform(points, 0, points, 0, points.length);
//		af.transform(points, points);
		bounds = null;
	}

	/**
	 * routine to return a Rectangle that describes the orthogonal box in polygon "poly".
	 * If the polygon is not an orthogonal box, returns null.
	 */
	public Rectangle2D.Double getBox()
	{
		/* closed boxes must have exactly four points */
		if (points.length == 4)
		{
			/* only closed polygons and text can be boxes */
			if (style != Type.FILLED && style != Type.CLOSED && style != Type.TEXTBOX) return null;
		} else if (points.length == 5)
		{
			if (style != Type.OPENED && style != Type.OPENEDT1 && style != Type.OPENEDT2 &&
				style != Type.OPENEDT3 && style != Type.OPENEDO1) return null;
			if (points[0].getX() != points[4].getX() || points[0].getY() != points[4].getY()) return null;
		} else return null;

		/* make sure the polygon is rectangular and orthogonal */
		if (points[0].getX() == points[1].getX() && points[2].getX() == points[3].getX() &&
			points[0].getY() == points[3].getY() && points[1].getY() == points[2].getY())
		{
			double cX = (points[2].getX() + points[0].getX()) / 2;
			double cY = (points[1].getY() + points[0].getY()) / 2;
			double sX, sY;
			if (points[0].getX() < points[2].getX())
			{
				sX = points[2].getX() - points[0].getX();
			} else
			{
				sX = points[0].getX() - points[1].getX();
			}
			if (points[0].getY() < points[1].getY())
			{
				sY = points[1].getY() - points[0].getY();
			} else
			{
				sY = points[0].getY() - points[1].getY();
			}
			return new Rectangle2D.Double(cX, cY, sX, sY);
		}
		if (points[0].getX() == points[3].getX() && points[1].getX() == points[2].getX() &&
			points[0].getY() == points[1].getY() && points[2].getY() == points[3].getY())
		{
			double cX = (points[1].getX() + points[0].getX()) / 2;
			double cY = (points[2].getY() + points[0].getY()) / 2;
			double sX, sY;
			if (points[0].getX() < points[1].getX())
			{
				sX = points[1].getX() - points[0].getX();
			} else
			{
				sX = points[0].getX() - points[1].getX();
			}
			if (points[0].getY() < points[2].getY())
			{
				sY = points[2].getY() - points[0].getY();
			} else
			{
				sY = points[0].getY() - points[2].getY();
			}
			return new Rectangle2D.Double(cX, cY, sX, sY);
		}
		return null;
	}

	/**
	 * routine to return true if (X,Y) is inside of polygon "poly"
	 */
	public boolean isinside(Point2D.Double pt)
	{
		if (style == Type.FILLED || style == Type.CLOSED || style == Type.CROSSED || style == Type.TEXTBOX)
		{
				/* check rectangular case for containment */
			Rectangle2D.Double bounds = getBox();
			if (bounds != null)
			{
				if (bounds.contains(pt)) return true;
				return false;
			}

			/* general polygon containment by summing angles to vertices */
			double ang = 0;
			Point2D.Double lastPoint = points[points.length-1];
			if (pt.equals(lastPoint)) return true;
			double lastp = EMath.figureAngle(pt, lastPoint);
			for(int i=0; i<points.length; i++)
			{
				Point2D.Double thisPoint = points[i];
				if (pt.equals(thisPoint)) return true;
				double thisp = EMath.figureAngle(pt, thisPoint);
				double tang = lastp - thisp;
				if (tang < -180) tang += 360;
				if (tang > 180) tang -= 360;
				ang += tang;
				lastp = thisp;
			}
			if (Math.abs(ang) <= points.length) return false;
			return true;
		}

		if (style == Type.CROSS || style == Type.BIGCROSS)
		{
			if (getCenterX() == pt.getX() && getCenterY() == pt.getY()) return true;
			return false;
		}

		if (style == Type.OPENED || style == Type.OPENEDT1 || style == Type.OPENEDT2 ||
			style == Type.OPENEDT3 || style == Type.VECTORS)
		{
			/* first look for trivial inclusion by being a vertex */
			for(int i=0; i<points.length; i++)
				if (pt.equals(points[i])) return true;

			/* see if the point is on one of the edges */
			if (style == Type.VECTORS)
			{
				for(int i=0; i<points.length; i += 2)
					if (EMath.isOnLine(points[i], points[i+1], pt)) return true;
			} else
			{
				for(int i=1; i<points.length; i++)
					if (EMath.isOnLine(points[i-1], points[i], pt)) return true;
			}
			return false;
		}

		if (style == Type.CIRCLE || style == Type.THICKCIRCLE || style == Type.DISC)
		{
			double dist = EMath.computeDistance(points[0], points[1]);
			double odist = EMath.computeDistance(points[0], pt);
			if (odist < dist) return true;
			return false;
		}

		if (style == Type.CIRCLEARC || style == Type.THICKCIRCLEARC)
		{
			/* first see if the point is at the proper angle from the center of the arc */
			double ang = EMath.figureAngle(points[0], pt);
			double endangle = EMath.figureAngle(points[0], points[1]);
			double startangle = EMath.figureAngle(points[0], points[2]);
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

			/* now see if the point is the proper distance from the center of the arc */
			double dist = EMath.computeDistance(points[0], pt);
			double wantdist;
			if (ang == startangle || angrange == 0)
			{
				wantdist = EMath.computeDistance(points[0], points[1]);
			} else if (ang == endangle)
			{
				wantdist = EMath.computeDistance(points[0], points[2]);
			} else
			{
				double startdist = EMath.computeDistance(points[0], points[1]);
				double enddist = EMath.computeDistance(points[0], points[2]);
				if (enddist == startdist) wantdist = startdist; else
				{
					wantdist = startdist + (ang - startangle) / angrange *
						(enddist - startdist);
				}
			}
			if (dist == wantdist) return true;
			return false;
		}

		/* I give up */
		return false;
	}

	// SHAPE REQUIREMENTS:
	/** Returns true if point (x,y) is contained in the poly. */
	public boolean contains(double x, double y)
	{
		return isinside(new Point2D.Double(x, y));
	}

	/** Returns true if point "p" is contained in the poly. */
	public boolean contains(Point2D p)
	{
		return isinside(new Point2D.Double(p.getX(), p.getY()));
	}

	/** TODO: write contains(double, double, double, double); */
	public boolean contains(double x, double y, double w, double h)
	{
		return false;
	}

	/** TODO: write contains(Rectangle2D); */
	public boolean contains(Rectangle2D r)
	{
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/** TODO: write intersects(double, double, double, double); */
	public boolean intersects(double x, double y, double w, double h)
	{
		return false;
	}

	/** TODO: write intersects(Rectangle2D); */
	public boolean intersects(Rectangle2D r)
	{
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/** Get the x coordinate of the center of this poly */
	public double getCenterX()
	{
		Rectangle2D b = getBounds2D();
		return b.getCenterX();
	}

	/** Get the y coordinate of the center of this poly */
	public double getCenterY()
	{
		Rectangle2D b = getBounds2D();
		return b.getCenterY();
	}

	/** Get the bounds of this poly, as a Rectangle2D */
	public Rectangle2D getBounds2D()
	{
		if (bounds == null) calcBounds();
		return bounds;
	}

	/** Get the bounds of this poly, as a Rectangle2D */
	public Rectangle2D.Double getBounds2DDouble()
	{
		if (bounds == null) calcBounds();
		return bounds;
	}

	/** Get the bounds of this poly, as a Rectangle */
	public Rectangle getBounds()
	{
		if (bounds == null) calcBounds();
		Rectangle2D r = getBounds2D();
		return new Rectangle((int)r.getX(), (int)r.getY(), (int)r.getWidth(), (int)r.getHeight());
	}

	protected void calcBounds()
	{
		double lx, ly, hx, hy;
		Rectangle2D sum;

		bounds = new Rectangle2D.Double();
		for (int i = 0; i < points.length; i++)
		{
			if (i == 0) bounds.setRect(points[0].getX(), points[0].getY(), 0, 0); else
				bounds.add(points[i]);
		}
	}

	class PolyPathIterator implements PathIterator
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

	/** Get a PathIterator for this poly after a transform */
	public PathIterator getPathIterator(AffineTransform at)
	{
		return new PolyPathIterator(this, at);
	}

	/** Get a PathIterator for this poly after a transform, with a particular
	 * flatness */
	public PathIterator getPathIterator(AffineTransform at, double flatness)
	{
		return getPathIterator(at);
	}
}
