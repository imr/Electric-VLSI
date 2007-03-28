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

import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.EditWindow0;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.AbstractShapeBuilder;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;

import java.awt.Font;
import java.awt.font.GlyphVector;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Class to define a polygon of points.
 */
public class Poly extends PolyBase {
    public static final Poly[] NULL_ARRAY = {};

	/** the string (if of type TEXT) */						private String string;
	/** the text descriptor (if of type TEXT) */			private TextDescriptor descript;
	/** the ElectricObject/Variable (if of type TEXT) */	private DisplayedText dt;

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
	 * Method to return the DisplayedText associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * @return the DisplayedText associated with this Poly.
	 */
	public DisplayedText getDisplayedText() { return dt; }

	/**
	 * Method to set the DisplayedText associated with this Poly.
	 * This only applies to text Polys which display a message.
	 * @param dt the DisplayedText associated with this Poly.
	 */
	public void setDisplayedText(DisplayedText dt) { this.dt = dt; }

    private static final int [] extendFactor = {0,
    11459, 5729, 3819, 2864, 2290, 1908, 1635, 1430, 1271, 1143,
    1039,  951,  878,  814,  760,  712,  669,  631,  598,  567,
    540,  514,  492,  470,  451,  433,  417,  401,  387,  373,
    361,  349,  338,  327,  317,  308,  299,  290,  282,  275,
    267,  261,  254,  248,  241,  236,  230,  225,  219,  214,
    210,  205,  201,  196,  192,  188,  184,  180,  177,  173,
    170,  166,  163,  160,  157,  154,  151,  148,  146,  143,
    140,  138,  135,  133,  130,  128,  126,  123,  121,  119,
    117,  115,  113,  111,  109,  107,  105,  104,  102,  100};

	/**
	 * Method to return the amount that an arc end should extend, given its width and extension factor.
	 * @param width the width of the arc.
	 * @param extend the extension factor (from 0 to 90).
	 * @return the extension (from 0 to half of the width).
	 */
    public static double getExtendFactor(double width, int extend) {
        return extend <= 0 || extend >= 90 ? width*0.5 : width * 50 / extendFactor[extend];
    }

	/**
	 * Method to construct a Poly for an arc with a given length, width, angle, endpoint, and extension.
	 * @param len the length of the arc.
	 * @param wid the width of the arc.
	 * @param angle the angle of the arc.
	 * @param endH the head end of the arc.
	 * @param extendH the head end extension distance of the arc.
	 * @param endT the tail end of the arc.
	 * @param extendT the tail end extension distance of the arc.
	 * @param style the style of the polygon (filled, opened, etc.)
	 * @return a Poly describing the outline of the arc.
	 */
	public static Poly makeEndPointPoly(double len, double wid, int angle, Point2D endH, double extendH,
		Point2D endT, double extendT, Poly.Type style)
	{
		double w2 = wid / 2;
		double x1 = endH.getX();   double y1 = endH.getY();
		double x2 = endT.getX();   double y2 = endT.getY();
		Point2D.Double [] points = null;

		// somewhat simpler if rectangle is manhattan
		if (angle == 900 || angle == 2700)
		{
			if (angle == 900)
//			if (y1 > y2)
			{
				double temp = y1;   y1 = y2;   y2 = temp;
				temp = extendH;   extendH = extendT;   extendT = temp;
			}
			points = new Point2D.Double[] {
				new Point2D.Double(x1 - w2, y1 - extendH),
				new Point2D.Double(x1 + w2, y1 - extendH),
				new Point2D.Double(x2 + w2, y2 + extendT),
				new Point2D.Double(x2 - w2, y2 + extendT)};
		} else if (angle == 0 || angle == 1800)
		{
			if (angle == 0)
//			if (x1 > x2)
			{
				double temp = x1;   x1 = x2;   x2 = temp;
				temp = extendH;   extendH = extendT;   extendT = temp;
			}
			points = new Point2D.Double[] {
				new Point2D.Double(x1 - extendH, y1 - w2),
				new Point2D.Double(x1 - extendH, y1 + w2),
				new Point2D.Double(x2 + extendT, y2 + w2),
				new Point2D.Double(x2 + extendT, y2 - w2)};
		} else
		{
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
			points = new Point2D.Double[] {
				new Point2D.Double(yextra + xe1, ye1 - xextra),
				new Point2D.Double(xe1 - yextra, xextra + ye1),
				new Point2D.Double(xe2 - yextra, xextra + ye2),
				new Point2D.Double(yextra + xe2, ye2 - xextra)};
		}
		if (wid != 0 && style.isOpened())
		{
			points = new Point2D.Double[] {points[0], points[1], points[2], points[3], points[0]};
		}
		Poly poly = new Poly(points);
		poly.setStyle(style);
		return poly;
	}

	/**
	 * Method to convert text Polys to their precise bounds in a given window.
	 * @param wnd the window.
	 * @param eObj the ElectricObject on which this text resides.
	 * If that ElectricObject is a NodeInst and the node is rotated, it affects the text anchor point.
	 * @return true if the text is too small to display.
	 */
	public boolean setExactTextBounds(EditWindow0 wnd, ElectricObject eObj)
	{
		if (getString() == null) return true;
		String theString = getString().trim();
		if (theString.length() == 0) return true;
		int numLines = 1;
		if (dt != null)
		{
			Variable var = dt.getVariable();
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
		}

		Type style = getStyle();
		style = rotateType(style, eObj);
		Font font = descript != null ? descript.getFont(wnd, 0) : TextDescriptor.getDefaultFont();
		if (font == null)
		{
			UserInterface ui = Job.getUserInterface();
			double size = ui.getDefaultTextSize();
			if (descript != null) size = descript.getTrueSize(wnd);
			size = size/wnd.getScale();
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
		GlyphVector gv = TextDescriptor.getGlyphs(theString, font);
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
//System.out.println("STRING '"+theString+"' STYLE="+getStyle().name+" ROTSTYLE="+style.name+" FROM ("+cX+","+cY+") HAS CORNER ("+corner.getX()+","+corner.getY()+")");
		cX = corner.getX();
		cY = corner.getY();
		double width = glyphBounds.getWidth() * textScale;
		double height = font.getSize() * textScale * numLines;
		switch (descript.getRotation().getIndex())
		{
			case 1:		// rotate 90 counterclockwise
				double saveWidth = width;
				width = -height;
				height = saveWidth;
				break;
			case 2:		// rotate 180
				width = -width;
				height = -height;
				break;
			case 3:		// rotate 90 clockwise
				double saveHeight = height;
				height = -width;
				width = saveHeight;
				break;
		}
		points = new Point2D.Double[] {
			new Point2D.Double(cX, cY),
			new Point2D.Double(cX+width, cY),
			new Point2D.Double(cX+width, cY+height),
			new Point2D.Double(cX, cY+height)};
		this.bounds = null;
        gv = null;  // for GC and glyphBounds
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
					offX = -offY;
					offY = saveOffX;
					break;
				case 2:
					offX = -offX;
					offY = -offY;
					break;
				case 3:
					offX = offY;
					offY = -saveOffX;
					break;
			}
		}
		return new Point2D.Double(cX+offX, cY+offY);
	}

    /**
     * Returns new instance of Poly builder to build shapes in lambda units.
     * @return new instance of Poly builder.
     */
    public static Builder newLambdaBuilder() {
        return new Builder(true);
    }
    
    /**
     * Returns new instance of Poly builder to build shapes in grid units.
     * @return new instance of Poly builder.
     */
    public static Builder newGridBuilder() {
        return new Builder(false);
    }
    
    /**
     * Returns thread local instance of Poly builder to build shapes in lambda units.
     * @return thread local instance of Poly builder.
     */
    public static Builder threadLocalLambdaBuilder() {
        return threadLocalLambdaBuilder.get();
    }
    
    private static ThreadLocal<Poly.Builder> threadLocalLambdaBuilder = new ThreadLocal<Poly.Builder>() {
        protected Poly.Builder initialValue() { return Poly.newLambdaBuilder(); }
    };

    /**
     * This class builds shapes of nodes and arcs in lambda units as Poly arrays.
     */
    public static class Builder extends AbstractShapeBuilder {
        private final boolean inLambda;
        private boolean isChanging;
        private final ArrayList<Poly> lastPolys = new ArrayList<Poly>();
        
        private Builder(boolean inLambda) { this.inLambda = inLambda; }
        
        /**
         * Returns the polygons that describe node "ni".
         * @param ni the NodeInst that is being described.
         * The prototype of this NodeInst must be a PrimitiveNode and not a Cell.
         * @return an iterator on Poly objects that describes this NodeInst graphically.
         */
        public Iterator<Poly> getShape(NodeInst ni) {
            Poly[] polys = ((PrimitiveNode)ni.getProto()).getTechnology().getShapeOfNode(ni, electrical, reasonable, onlyTheseLayers);
            lastPolys.clear();
            for (Poly poly: polys) {
                if (!inLambda)
                    poly.lambdaToGrid();
                lastPolys.add(poly);
            }
            return lastPolys.iterator();
        }
        
        /**
         * Returns the polygons that describe arc "ai".
         * @param ai the ArcInst that is being described.
         * @return an iterator on Poly objects that describes this ArcInst graphically.
         */
        public Iterator<Poly> getShape(ArcInst ai) {
            isChanging = true;
            setShrinkage(ai.getParent().getShrinkage());
            lastPolys.clear();
            genShapeOfArc(ai.getD());
            if (inLambda) {
                for (int i = 0; i < lastPolys.size(); i++)
                    lastPolys.get(i).gridToLambda();
            }
            isChanging = false;
            return lastPolys.iterator();
        }
        
        /**
         * Returns the polygons that describe arc "ai".
         * @param ai the ArcInst that is being described.
         * @return an array of Poly objects that describes this ArcInst graphically.
         */
    	public Poly [] getShapeArray(ArcInst ai) {
            isChanging = true;
            setShrinkage(ai.getParent().getShrinkage());
            lastPolys.clear();
            genShapeOfArc(ai.getD());
            if (lastPolys.isEmpty()) {
                isChanging = false;
                return Poly.NULL_ARRAY;
            }
            Poly[] polys = new Poly[lastPolys.size()];
            if (inLambda) {
                for (int i = 0; i < polys.length; i++) {
                    Poly poly = lastPolys.get(i);
                    poly.gridToLambda();
                    polys[i] = poly;
                }
            } else {
                for (int i = 0; i < polys.length; i++)
                    polys[i] = lastPolys.get(i);
            }
            isChanging = false;
            return polys;
        }
        
        /**
         * Method to create a Poly object that describes an ImmutableArcInst.
         * The ImmutableArcInst is described by its width and style.
         * @param a an ImmutableArcInst
         * @param gridWidth the width of the Poly in grid units.
         * @param style the style of the ArcInst.
         * @return a Poly that describes the ArcInst.
         */
        public Poly makePoly(ImmutableArcInst a, long gridWidth, Poly.Type style) {
            isChanging = true;
            lastPolys.clear();
            makeGridPoly(a, gridWidth, style, null);
            isChanging = false;
            if (lastPolys.isEmpty()) return null;
            Poly poly = lastPolys.get(0);
            if (inLambda)
                poly.gridToLambda();
            return poly;
        }
        
        @Override
        public void addDoublePoly(int numPoints, Poly.Type style, Layer layer) {
            assert isChanging;
            Point2D.Double[] points = new Point2D.Double[numPoints];
            for (int i = 0; i < numPoints; i++)
                points[i] = new Point2D.Double(doubleCoords[i*2], doubleCoords[i*2+1]);
            Poly poly = new Poly(points);
            poly.setStyle(style);
            poly.setLayer(layer);
            lastPolys.add(poly);
        }
        
        @Override
        public void addIntLine(int[] coords, Poly.Type style, Layer layer) {
            assert isChanging;
            Poly poly = new Poly(new Point2D.Double[] {
                new Point2D.Double(coords[0], coords[1]),
                new Point2D.Double(coords[2], coords[3])
            });
            poly.setStyle(style);
            poly.setLayer(layer);
            lastPolys.add(poly);
        }
        
        @Override
        public void addIntBox(int[] coords, Layer layer) {
            assert isChanging;
            Poly poly = new Poly(new Point2D.Double[] {
                new Point2D.Double(coords[0], coords[1]),
                new Point2D.Double(coords[2], coords[1]),
                new Point2D.Double(coords[2], coords[3]),
                new Point2D.Double(coords[0], coords[3])
            });
            poly.setStyle(Poly.Type.FILLED);
            poly.setLayer(layer);
            lastPolys.add(poly);
        }
    }
    
	/**
	 * Type is a typesafe enum class that describes the nature of a Poly.
	 */
	public static enum Type
	{
		// ************************ polygons ************************
		/**
		 * Describes a closed polygon which is filled in.
		 */
		FILLED("filled", false),
		/**
		 * Describes a closed polygon with only the outline drawn.
		 */
		CLOSED("closed", false),
		/**
		 * Describes a closed rectangle with the outline drawn and an "X" drawn through it.
		 */
		CROSSED("crossed", false),

		// ************************ lines ************************
		/**
		 * Describes an open outline.
		 * The last point is not implicitly connected to the first point.
		 */
		OPENED("opened", false),
		/**
		 * Describes an open outline, drawn with a dotted texture.
		 * The last point is not implicitly connected to the first point.
		 */
		OPENEDT1("opened-dotted", false),
		/**
		 * Describes an open outline, drawn with a dashed texture.
		 * The last point is not implicitly connected to the first point.
		 */
		OPENEDT2("opened-dashed", false),
		/**
		 * Describes an open outline, drawn with thicker lines.
		 * The last point is not implicitly connected to the first point.
		 */
		OPENEDT3("opened-thick", false),
		/**
		 * Describes a vector endpoint pairs, solid.
		 * There must be an even number of points in the Poly so that vectors can be drawn from point 0 to 1,
		 * then from point 2 to 3, etc.
		 */
		VECTORS("vectors", false),

		// ************************ curves ************************
		/**
		 * Describes a circle (only the outline is drawn).
		 * The first point is the center of the circle and the second point is on the edge, thus defining the radius.
		 * This second point should be on the same horizontal level as the radius point to make radius computation easier.
		 */
		CIRCLE("circle", false),
		/**
		 * Describes a circle, drawn with thick lines (only the outline is drawn).
		 * The first point is the center of the circle and the second point is on the edge, thus defining the radius.
		 * This second point should be on the same horizontal level as the radius point to make radius computation easier.
		 */
		THICKCIRCLE("thick-circle", false),
		/**
		 * Describes a filled circle.
		 * The first point is the center of the circle and the second point is on the edge, thus defining the radius.
		 * This second point should be on the same horizontal level as the radius point to make radius computation easier.
		 */
		DISC("disc", false),
		/**
		 * Describes an arc of a circle.
		 * The first point is the center of the circle, the second point is the start of the arc, and
		 * the third point is the end of the arc.
		 * The arc will be drawn counter-clockwise from the start point to the end point.
		 */
		CIRCLEARC("circle-arc", false),
		/**
		 * Describes an arc of a circle, drawn with thick lines.
		 * The first point is the center of the circle, the second point is the start of the arc, and
		 * the third point is the end of the arc.
		 * The arc will be drawn counter-clockwise from the start point to the end point.
		 */
		THICKCIRCLEARC("thick-circle-arc", false),

		// ************************ text ************************
		/**
		 * Describes text that should be centered about the Poly point.
		 * Only one point need be specified.
		 */
		TEXTCENT("text-center", true),
		/**
		 * Describes text that should be placed so that the Poly point is at the top-center.
		 * Only one point need be specified, and the text will be below that point.
		 */
		TEXTTOP("text-top", true),
		/**
		 * Describes text that should be placed so that the Poly point is at the bottom-center.
		 * Only one point need be specified, and the text will be above that point.
		 */
		TEXTBOT("text-bottom", true),
		/**
		 * Describes text that should be placed so that the Poly point is at the left-center.
		 * Only one point need be specified, and the text will be to the right of that point.
		 */
		TEXTLEFT("text-left", true),
		/**
		 * Describes text that should be placed so that the Poly point is at the right-center.
		 * Only one point need be specified, and the text will be to the left of that point.
		 */
		TEXTRIGHT("text-right", true),
		/**
		 * Describes text that should be placed so that the Poly point is at the upper-left.
		 * Only one point need be specified, and the text will be to the lower-right of that point.
		 */
		TEXTTOPLEFT("text-topleft", true),
		/**
		 * Describes text that should be placed so that the Poly point is at the lower-left.
		 * Only one point need be specified, and the text will be to the upper-right of that point.
		 * This is the normal starting point for most text.
		 */
		TEXTBOTLEFT("text-botleft", true),
		/**
		 * Describes text that should be placed so that the Poly point is at the upper-right.
		 * Only one point need be specified, and the text will be to the lower-left of that point.
		 */
		TEXTTOPRIGHT("text-topright", true),
		/**
		 * Describes text that should be placed so that the Poly point is at the lower-right.
		 * Only one point need be specified, and the text will be to the upper-left of that point.
		 */
		TEXTBOTRIGHT("text-botright", true),
		/**
		 * Describes text that is centered in the Poly and must remain inside.
		 * If the letters do not fit, a smaller font will be used, and if that still does not work,
		 * any letters that cannot fit are not written.
		 * The Poly coordinates must define an area for the text to live in.
		 */
		TEXTBOX("text-box", true),

		// ************************ miscellaneous ************************
		/**
		 * Describes a small cross, drawn at the specified location.
		 * Typically there will be only one point in this polygon
		 * but if there are more they are averaged and the cross is drawn in the center.
		 */
		CROSS("cross", false),
		/**
		 * Describes a big cross, drawn at the specified location.
		 * Typically there will be only one point in this polygon
		 * but if there are more they are averaged and the cross is drawn in the center.
		 */
		BIGCROSS("big-cross", false);

		private final String name;
		private final boolean isText;

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

		/**
		 * Method to tell whether this is a style that can draw an opened polygon.
		 * @return true if this is a style that can draw an opened polygon.
		 */
		public boolean isOpened()
		{
			if (this == OPENED || this == OPENEDT1 || this == OPENEDT2 ||
				this == OPENEDT3 || this == VECTORS) return true;
			return false;
		}

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
