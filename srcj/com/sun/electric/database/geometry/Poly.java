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

import com.sun.electric.database.text.Name;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.Font;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class Poly extends PolyBase {

	/** the string (if of type TEXT) */						private String string;
	/** the Name (if of type TEXT) */						private Name name;
	/** the text descriptor (if of type TEXT) */			private TextDescriptor descript;
	/** the variable (if of type TEXT) */					private Variable var;

	/**
	 * The constructor creates a new Poly given an array of points.
	 * @param points the array of coordinates.
	 */
	public Poly(Point2D [] points)
	{
		super(points);
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
		super(cX, cY, width, height);
	}

	/**
	 * The constructor creates a new Poly that describes a rectangle.
	 * @param rect the Rectangle2D of the rectangle.
	 */
	public Poly(Rectangle2D rect)
	{
		super(rect);
	}

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
	 * Method to construct a Poly for an arc with a given length, width, angle, endpoint, and extension.
	 * @param len the length of the arc.
	 * @param wid the width of the arc.
	 * @param angle the angle of the arc.
	 * @param endH the head end of the arc.
	 * @param extendH the head end extension distance of the arc.
	 * @param endT the tail end of the arc.
	 * @param extendT the tail end extension distance of the arc.
	 * @return a Poly describing the outline of the arc.
	 */
	public static Poly makeEndPointPoly(double len, double wid, int angle, Point2D endH, double extendH,
		Point2D endT, double extendT)
	{
		double w2 = wid / 2;
		double x1 = endH.getX();   double y1 = endH.getY();
		double x2 = endT.getX();   double y2 = endT.getY();

		// somewhat simpler if rectangle is manhattan
		if (angle == 900 || angle == 2700)
		{
			if (y1 > y2)
			{
				double temp = y1;   y1 = y2;   y2 = temp;
				temp = extendH;   extendH = extendT;   extendT = temp;
			}
			new Poly(new Point2D.Double[] {
				new Point2D.Double(x1 - w2, y1 - extendH),
				new Point2D.Double(x1 + w2, y1 - extendH),
				new Point2D.Double(x2 + w2, y2 + extendT),
				new Point2D.Double(x2 - w2, y2 + extendT)});
		}
		if (angle == 0 || angle == 1800)
		{
			if (x1 > x2)
			{
				double temp = x1;   x1 = x2;   x2 = temp;
				temp = extendH;   extendH = extendT;   extendT = temp;
			}
			return new Poly(new Point2D.Double[] {
				new Point2D.Double(x1 - extendH, y1 - w2),
				new Point2D.Double(x1 - extendH, y1 + w2),
				new Point2D.Double(x2 + extendT, y2 + w2),
				new Point2D.Double(x2 + extendT, y2 - w2)});
		}

		// nonmanhattan arcs cannot have zero length so re-compute it
		if (len == 0) len = endH.distance(endT);
		double xextra, yextra, xe1, ye1, xe2, ye2;
		if (len == 0)
		{
			double sa = DBMath.sin(angle);
			double ca = DBMath.cos(angle);
			xe1 = x1 - ca * extendH;
			ye1 = y1 - sa * extendH;
			xe2 = x2 + ca * extendT;
			ye2 = y2 + sa * extendT;
			xextra = ca * w2;
			yextra = sa * w2;
		} else
		{
			// work out all the math for nonmanhattan arcs
			xe1 = x1 - extendH * (x2-x1) / len;
			ye1 = y1 - extendH * (y2-y1) / len;
			xe2 = x2 + extendT * (x2-x1) / len;
			ye2 = y2 + extendT * (y2-y1) / len;

			// now compute the corners
			xextra = w2 * (x2-x1) / len;
			yextra = w2 * (y2-y1) / len;
		}
		return new Poly(new Point2D.Double[] {
			new Point2D.Double(yextra + xe1, ye1 - xextra),
			new Point2D.Double(xe1 - yextra, xextra + ye1),
			new Point2D.Double(xe2 - yextra, xextra + ye2),
			new Point2D.Double(yextra + xe2, ye2 - xextra)});
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
		if (getString() == null) return true;
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

		Type style = getStyle();
		style = rotateType(style, eObj);
		Font font = wnd.getFont(descript);
		if (font == null)
		{
			double size = EditWindow.getDefaultFontSize();
			if (descript != null) size = descript.getTrueSize(wnd);
			size = wnd.getTextUnitSize(size);
			if (size <= 0) size = 1;
			double cX = getBounds2D().getCenterX();
			double cY = getBounds2D().getCenterY();
			double sizeIndent = size / 4;
			double fakeWidth = theString.length() * size * 0.75;
			Point2D pt = getTextCorner(style, cX, cY, fakeWidth, size);
			cX = pt.getX();   cY = pt.getY();
			points = new Point2D.Double[] {
				new Point2D.Double(cX, cY+sizeIndent),
				new Point2D.Double(cX+fakeWidth, cY+sizeIndent),
				new Point2D.Double(cX+fakeWidth, cY+size-sizeIndent),
				new Point2D.Double(cX, cY+size-sizeIndent)};
			this.bounds = null;
			return false;
		}
		Rectangle2D bounds = getBounds2D();
		double lX = bounds.getMinX();
		double hX = bounds.getMaxX();
		double lY = bounds.getMinY();
		double hY = bounds.getMaxY();
		GlyphVector gv = wnd.getGlyphs(theString, font);
		Rectangle2D glyphBounds = gv.getVisualBounds();

		// adjust to place text in the center
		double textScale = getTextScale(wnd, gv, style, lX, hX, lY, hY);
		double textWidth = glyphBounds.getWidth();
		double textHeight = font.getSize();
		double scaledWidth = textWidth * textScale;
		double scaledHeight = textHeight * textScale;
		double cX = (lX + hX) / 2;
		double cY = (lY + hY) / 2;
		Point2D corner = getTextCorner(style, cX, cY, scaledWidth, scaledHeight);
		cX = corner.getX();
		cY = corner.getY();
		double width = glyphBounds.getWidth() * textScale;
		double height = font.getSize() * textScale * numLines;
		switch (descript.getRotation().getIndex())
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
	 * Method to return the coordinates of the lower-left corner of text in a window.
	 * @param style the anchor information for the text.
	 * @param cX the center X bound of the polygon containing the text.
	 * @param cY the center Y bound of the polygon containing the text.
	 * @param scaledWidth the width of the polygon containing the text.
	 * @param scaledHeight the height of the polygon containing the text.
	 * @return the coordinates of the lower-left corner of the text.
	 */
	private Point2D getTextCorner(Poly.Type style, double cX, double cY, double scaledWidth, double scaledHeight)
	{
		double offX = 0, offY = 0;
		if (style == Type.TEXTCENT || style == Type.TEXTBOX)
		{
			offX = -scaledWidth/2;
			offY = -scaledHeight/2;
		} else if (style == Type.TEXTTOP)
		{
			offX = -scaledWidth/2;
			offY = -scaledHeight;
		} else if (style == Type.TEXTBOT)
		{
			offX = -scaledWidth/2;
		} else if (style == Type.TEXTLEFT)
		{
			offY = -scaledHeight/2;
		} else if (style == Type.TEXTRIGHT)
		{
			offX = -scaledWidth;
			offY = -scaledHeight/2;
		} else if (style == Type.TEXTTOPLEFT)
		{
			offY = -scaledHeight;
		} else if (style == Type.TEXTBOTLEFT)
		{
		} else if (style == Type.TEXTTOPRIGHT)
		{
			offX = -scaledWidth;
			offY = -scaledHeight;
		} else if (style == Type.TEXTBOTRIGHT)
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
		return new Point2D.Double(cX+offX, cY+offY);
	}

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
		 * @param angle of the text anchor.
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
}
