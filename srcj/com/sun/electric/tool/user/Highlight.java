/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Highlight.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;

import java.awt.*;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

/*
 * Class for highlighting of objects on the display.
 * <P>
 * These are the types of highlighting that can occur:
 * <UL>
 * <LI>EOBJ: an ElectricObject is selected (NodeInst, ArcInst, or PortInst).
 *   <UL>
 *   <LI>Fills in "eobj" and the parent "cell".
 *   <LI>If selecting a NodeInst, may fill-in "point" if an outline node is being edited.
 *   </UL>
 * <LI>TEXT: text is selected.
 *   <UL>
 *   <LI>Fills in "eobj" and the parent "cell".
 *   <LI>If "var" is valid, this is a variable on a NodeInst, ArcInst, Export, PortInst, or Cell.
 *   <LI>If "var" is null and "name" is valid, it is the name of a NodeInst or ArcInst.
 *   <LI>If "var" and "name" are null and "eobj" is an Export, it is that Export.
 *   <LI>If "var" and "name" are null, this is a Cell instance name.
 *   </UL>
 * <LI>BBOX: a rectangular area is selected.  Fills in "bounds" and the parent "cell".
 * <LI>LINE: a line is selected.  Fills in "pt1", "pt2" and the parent "cell".
 * <LI>MESSAGE: a random piece of text is displayed (not from the database.  Fills in "pt1", "msg" and the parent "cell".
 * </UL>
 */
public class Highlight
{
	/**
	 * Type is a typesafe enum class that describes the nature of the highlight.
	 */
	public static class Type
	{
		private final String name;
		private int order;
		private static int ordering = 1;

		private Type(String name) { this.name = name;   this.order = ordering++; }

		/**
		 * Returns an ordering of this Type.
		 * The ordering is used in the multi-object Get Info dialog.
		 * @return an ordering of this Type.
		 */
		public int getOrder() { return order; }

		/**
		 * Returns a printable version of this Type.
		 * @return a printable version of this Type.
		 */
		public String toString() { return name; }

		/** Describes a highlighted ElectricObject. */			public static final Type EOBJ = new Type("electricObject");
		/** Describes highlighted text. */						public static final Type TEXT = new Type("text");
		/** Describes a highlighted area. */					public static final Type BBOX = new Type("area");
		/** Describes a highlighted line. */					public static final Type LINE = new Type("line");
		/** Describes a thick highlighted line. */				public static final Type THICKLINE = new Type("thick line");
		/** Describes a non-database text. */					public static final Type MESSAGE = new Type("message");
        /** Describes a Polygon */                              public static final Type POLY = new Type("poly");
	}

	/** The type of the highlighting. */						private Type type;
	/** The highlighted object. */								private ElectricObject eobj;
	/** The Cell containing the selection. */					private Cell cell;
	/** The highlighted outline point (only for NodeInst). */	private int point;
	/** The highlighted variable. */							private Variable var;
	/** The highlighted Name. */								private Name name;
	/** The highlighted area. */								private Rectangle2D bounds;
	/** The highlighted line. */								private Point2D pt1, pt2;
	/** The center point about which thick lines revolve. */	private Point2D center;
	/** The highlighted message. */								private String msg;
    /** The highlighted polygon */                              private Poly polygon;
    /** The color used when drawing polygons */                 private Color color;
    /** For Highlighted networks, this prevents excess highlights */ private boolean highlightConnected;

    /** for drawing solid lines */		private static final BasicStroke solidLine = new BasicStroke(0);
    /** for drawing dotted lines */		private static final BasicStroke dottedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1}, 0);
    /** for drawing dashed lines */		private static final BasicStroke dashedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {10}, 0);
    private static final int CROSSSIZE = 3;

    /** You should be using factory methods from Highlighter instead of this */
    protected Highlight(Type type, ElectricObject eobj, Cell cell)
	{
		this.type = type;
		this.eobj = eobj;
		this.cell = cell;
		this.point = -1;
		this.var = null;
		this.name = null;
		this.bounds = null;
		this.pt1 = null;
		this.pt2 = null;
		this.msg = null;
        this.polygon = null;
        this.color = null;
        this.highlightConnected = true;
	}

    /**
	 * Method to return the type of this Highlight (EOBJ, TEXT, BBOX, LINE, or MESSAGE).
	 * @return the type of this Highlight.
	 */
	public Type getType() { return type; }

	/**
	 * Method to return the ElectricObject associated with this Highlight object.
	 * @return the ElectricObject associated with this Highlight object.
	 */
	public ElectricObject getElectricObject() { return eobj; }

	/**
	 * Method to set the ElectricObject associated with this Highlight object.
	 * @param eobj the ElectricObject associated with this Highlight object.
	 */
	protected void setElectricObject(ElectricObject eobj) { this.eobj = eobj; }

	/**
	 * Method to return the Cell associated with this Highlight object.
	 * @return the Cell associated with this Highlight object.
	 */
	public Cell getCell() { return cell; }

	/**
	 * Method to set the Cell associated with this Highlight object.
	 * @param cell the Cell associated with this Highlight object.
	 */
	//private void setCell(Cell cell) { this.cell = cell; }

	/**
	 * Method to return the outline point associated with this Highlight object.
	 * @return the outline point associated with this Highlight object.
	 */
	public int getPoint() { return point; }

	/**
	 * Method to set an outline point to be displayed with this Highlight.
	 * @param point the outline point to show with this Highlight (must be a NodeInst highlight).
	 */
	public void setPoint(int point) { this.point = point; }

	/**
	 * Method to return the bounds associated with this Highlight object.
	 * Bounds are used for area definitions and also for text.
	 * @return the bounds associated with this Highlight object.
	 */
	public Rectangle2D getBounds() { return bounds; }

    /**
     * Method to set the bounds associated with this Highlight object.
     * Bounds are used for area definitions and also for text.
     * @param bounds the bounds associated with this Highlight object.
     */
    protected void setBounds(Rectangle2D bounds) { this.bounds = bounds; }

	/**
	 * Method to return the Name associated with this Highlight object.
	 * @return the Name associated with this Highlight object.
	 */
	public Name getName() { return name; }

	/**
	 * Method to set the Name associated with this Highlight object.
	 * @param name the Name associated with this Highlight object.
	 */
	protected void setName(Name name) { this.name = name; }

	/**
	 * Method to return the Variable associated with this Highlight object.
	 * @return the Variable associated with this Highlight object.
	 */
	public Variable getVar() { return var; }

	/**
	 * Method to set the Variable associated with this Highlight object.
	 * @param var the Variable associated with this Highlight object.
	 */
	public void setVar(Variable var) { this.var = var; }

	/**
	 * Method to return the "from point" associated with this Highlight object.
	 * This only applies to Highlights of type LINE.
	 * @return the from point associated with this Highlight object.
	 */
	public Point2D getLineStart() { return pt1; }

    /**
     * Method to set the "from point" associated with this Highlight object.
     * This only applies to Highlights of type LINE.
     */
    public void setLineStart(Point2D pt) { pt1 = new Point2D.Double(pt.getX(), pt.getY()); }

	/**
	 * Method to return the "to point" associated with this Highlight object.
	 * This only applies to Highlights of type LINE.
	 * @return the to point associated with this Highlight object.
	 */
	public Point2D getLineEnd() { return pt2; }

    /**
     * Method to set the "to point" associated with this Highlight object.
     * This only applies to Highlights of type LINE.
     */
    public void setLineEnd(Point2D pt) { pt2 = new Point2D.Double(pt.getX(), pt.getY()); }

    /**
     * Method to get the message associated with this MESSAGE type Highlight.
     * @return the message
     */
    public String getMessage() { return msg; }

    /**
     * Method to set the message associated with this MESSAGE type Highlight.
     * @param msg
     */
    protected void setMessage(String msg) { this.msg = msg; }

    /**
     * Method to get the location of a message highlight.
     * This only applies to Highlights of type MESSAGE
     * @return the message location
     */
    public Point2D getLocation() { return pt1; }

    /**
     * Method to set the location of a message highlight.
     * This only applies to Highlights of type MESSAGE
     * @param pt the location
     */
    public void setLocation(Point2D pt) { pt1 = new Point2D.Double(pt.getX(), pt.getY()); }

    /**
     * Method to set the center point of a THICKLINE
     * @param pt the center point
     */
    protected void setCenter(Point2D pt) { center = pt; }

    /**
     * Method to get the center point of a THICKLINE
     * @return the center point
     */
    public Point2D getCenter() { return center; }

    /**
     * Method to set if objects connected to this should also be highlighted.
     * Should be set to false for Networks.
     * @param b true to highlight connected objects, false otherwise.
     */
    public void setHighlightConnected(boolean b) { highlightConnected = b; }

    /**
     * Sets the polygon of this POLY type highlight.
     * Has no effect if this is not a POLY type object.
     * @param poly the poly to use
     */
    public void setPoly(Poly poly) { polygon = poly; }

    /**
     * Get the Poly of this POLY type highlight.
     * Returns null for non-POLY type highlights.
     * @return the poly
     */
    public Poly getPoly() { return polygon; }

    /**
     * Sets the color of the Highlight.
     * Currently only used for Poly Types.
     * @param color the color to use
     */
    public void setColor(Color color) { this.color = color; }

    /**
     * Get the color of the Highlight.
     * Currently only valid for Poly Types.
     * @return the color to use
     */
    public Color getColor() { return color; }

    /**
     * Returns true if the highlight is still valid. Highlights are no longer valid
     * if the object they highlight has been removed from the database.
     * @return true if highlighted is still valid, false otherwise.
     */
    public boolean isValid() {

        if (!cell.isLinked()) return false;

        if (type == Type.EOBJ) {
            return eobj.isLinked();
        }
        if (type == Type.BBOX || type == Type.LINE || type == Type.MESSAGE ||
            type == Type.THICKLINE || type == Type.POLY) {
            return true;
        }
        if (type == Type.TEXT) {
            if (var != null) return var.isLinked();
            if (eobj != null) return eobj.isLinked();
            return false;
        }
        return false;
    }

    /**
	 * Method to tell whether this Highlight is text that stays with its node.
	 * The two possibilities are (1) text on invisible pins
	 * (2) export names, when the option to move exports with their labels is requested.
	 * @return true if this Highlight is text that should move with its node.
	 */
	public boolean nodeMovesWithText()
	{
		if (type != Type.TEXT) return false;
		if (var != null)
		{
			/* moving variable text */
			if (!(eobj instanceof NodeInst)) return false;
			NodeInst ni = (NodeInst)eobj;
			if (ni.isInvisiblePinWithText()) return true;
		} else
		{
			/* moving export text */
			if (!(eobj instanceof Export)) return false;
			Export pp = (Export)eobj;
			if (pp.getOriginalPort().getNodeInst().getProto() == Generic.tech.invisiblePinNode) return true;
			if (User.isMoveNodeWithExport()) return true;
		}
		return false;
	}

    /**
	 * Method to display this Highlight in a window.
	 * @param wnd the window in which to draw this highlight.
	 * @param g the Graphics associated with the window.
	 */
	public void showHighlight(EditWindow wnd, Graphics g, int highOffX, int highOffY, boolean showArcConstraints)
	{
        if (!isValid()) return;

		g.setColor(new Color(User.getColorHighlight()));
		if (type == Type.BBOX)
		{
			Point2D [] points = new Point2D.Double[5];
			points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
			points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
			points[2] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
			points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
			points[4] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
			drawOutlineFromPoints(wnd, g, points, highOffX, highOffY, false, null);
			return;
		}
		if (type == Type.LINE)
		{
			Point2D [] points = new Point2D.Double[2];
			points[0] = new Point2D.Double(pt1.getX(), pt1.getY());
			points[1] = new Point2D.Double(pt2.getX(), pt2.getY());
			drawOutlineFromPoints(wnd, g, points, highOffX, highOffY, false, null);
			return;
		}
		if (type == Type.THICKLINE)
		{
			Point2D [] points = new Point2D.Double[2];
			points[0] = new Point2D.Double(pt1.getX(), pt1.getY());
			points[1] = new Point2D.Double(pt2.getX(), pt2.getY());
			drawOutlineFromPoints(wnd, g, points, highOffX, highOffY, false, center);
			return;
		}
		if (type == Type.TEXT)
		{
			Point2D [] points = Highlighter.describeHighlightText(wnd, getElectricObject(), getVar(), getName());
			if (points == null) return;
			Point2D [] linePoints = new Point2D[2];
			for(int i=0; i<points.length; i += 2)
			{
				linePoints[0] = points[i];
				linePoints[1] = points[i+1];
				drawOutlineFromPoints(wnd, g, linePoints, highOffX, highOffY, false, null);
			}
			return;
		}
        if (type == Type.POLY) {
            // switch colors if specified
            Color oldColor = null;
            if (color != null) {
                oldColor = g.getColor();
                g.setColor(color);
            }
            // draw outline of poly
            boolean opened = (polygon.getStyle() == Poly.Type.OPENED);
            drawOutlineFromPoints(wnd, g, polygon.getPoints(), highOffX, highOffY, opened, null);
            // switch back to old color if switched
            if (oldColor != null)
                g.setColor(oldColor);
            return;
        }
		if (type == Type.MESSAGE)
		{
			Point loc = wnd.databaseToScreen(pt1.getX(), pt1.getY());
			g.drawString(msg, loc.x, loc.y);
		}

		// highlight ArcInst
		if (eobj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)eobj;

			// construct the polygons that describe the basic arc
			Poly poly = ai.makePoly(ai.getLength(), ai.getWidth() - ai.getProto().getWidthOffset(), Poly.Type.CLOSED);
			if (poly == null) return;
			drawOutlineFromPoints(wnd, g, poly.getPoints(), highOffX, highOffY, false, null);

			if (showArcConstraints)
			{
				// this is the only thing highlighted: give more information about constraints
				String constraints = "X";
				if (ai.isRigid()) constraints = "R"; else
				{
					if (ai.isFixedAngle())
					{
						if (ai.isSlidable()) constraints = "FS"; else
							constraints = "F";
					} else if (ai.isSlidable()) constraints = "S";
				}
				Point p = wnd.databaseToScreen(ai.getTrueCenterX(), ai.getTrueCenterY());
				Font font = wnd.getFont(null);
				if (font != null)
				{
					GlyphVector gv = wnd.getGlyphs(constraints, font);
					Rectangle2D glyphBounds = gv.getVisualBounds();
					g.drawString(constraints, (int)(p.x - glyphBounds.getWidth()/2 + highOffX),
						(int)(p.y + font.getSize()/2 + highOffY));
				}
			}
			return;
		}

		// highlight NodeInst
		PortProto pp = null;
		ElectricObject realEObj = eobj;
		if (realEObj instanceof PortInst)
		{
			pp = ((PortInst)realEObj).getPortProto();
			realEObj = ((PortInst)realEObj).getNodeInst();
		}
		if (realEObj instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)realEObj;
			NodeProto np = ni.getProto();
			AffineTransform trans = ni.rotateOutAboutTrueCenter();

            // draw nodeInst outline
            Poly niPoly = getNodeInstOutline(ni);
            boolean niOpened = (niPoly.getStyle() == Poly.Type.OPENED);
            drawOutlineFromPoints(wnd, g, niPoly.getPoints(), highOffX, highOffY, niOpened, null);

            int offX = highOffX;
            int offY = highOffY;
/*
			boolean drewOutline = false;
			if (np instanceof PrimitiveNode)
			{
				// special case for outline nodes
				if (np.isHoldsOutline()) 
				{
					Point2D [] outline = ni.getTrace();
					if (outline != null)
					{
						int numPoints = outline.length;
						Point2D [] pointList = new Point2D.Double[numPoints];
						for(int i=0; i<numPoints; i++)
						{
							pointList[i] = new Point2D.Double(ni.getTrueCenterX() + outline[i].getX(),
								ni.getTrueCenterY() + outline[i].getY());
						}
						trans.transform(pointList, 0, pointList, 0, numPoints);
						drawOutlineFromPoints(wnd, g, pointList, 0, 0, true, null);
						drewOutline = true;
					}
				}
			}

			// setup outline of node with standard offset
			if (!drewOutline)
			{
				SizeOffset so = ni.getSizeOffset();
				double nodeLowX = ni.getTrueCenterX() - ni.getXSize()/2 + so.getLowXOffset();
				double nodeHighX = ni.getTrueCenterX() + ni.getXSize()/2 - so.getHighXOffset();
				double nodeLowY = ni.getTrueCenterY() - ni.getYSize()/2 + so.getLowYOffset();
				double nodeHighY = ni.getTrueCenterY() + ni.getYSize()/2 - so.getHighYOffset();
				if (nodeLowX == nodeHighX && nodeLowY == nodeHighY)
				{
					float x = (float)nodeLowX;
					float y = (float)nodeLowY;
					float size = 3 / (float)wnd.getScale();
					Point c1 = wnd.databaseToScreen(x+size, y);
					Point c2 = wnd.databaseToScreen(x-size, y);
					Point c3 = wnd.databaseToScreen(x, y+size);
					Point c4 = wnd.databaseToScreen(x, y-size);
					drawLine(g, wnd, c1.x + offX, c1.y + offY, c2.x + offX, c2.y + offY);
					drawLine(g, wnd, c3.x + offX, c3.y + offY, c4.x + offX, c4.y + offY);
				} else
				{
					double nodeX = (nodeLowX + nodeHighX) / 2;
					double nodeY = (nodeLowY + nodeHighY) / 2;
					Poly poly = new Poly(nodeX, nodeY, nodeHighX-nodeLowX, nodeHighY-nodeLowY);
					poly.transform(trans);
					drawOutlineFromPoints(wnd, g, poly.getPoints(), offX, offY, false, null);
				}
			}
*/

			// draw the selected point
			if (point >= 0)
			{
				Point2D [] points = ni.getTrace();
				if (points != null)
				{
					boolean showWrap = ni.traceWraps();
					double x = ni.getAnchorCenterX() + points[point].getX();
					double y = ni.getAnchorCenterY() + points[point].getY();
					Point2D thisPt = new Point2D.Double(x, y);
					trans.transform(thisPt, thisPt);
					Point cThis = wnd.databaseToScreen(thisPt);
					int size = 3;
					drawLine(g, wnd, cThis.x + size + offX, cThis.y + size + offY, cThis.x - size + offX, cThis.y - size + offY);
					drawLine(g, wnd, cThis.x + size + offX, cThis.y - size + offY, cThis.x - size + offX, cThis.y + size + offY);

					// draw two connected lines
					Point2D prevPt = null, nextPt = null;
					int prevPoint = point - 1;
					if (prevPoint < 0 && showWrap) prevPoint = points.length - 1;
					if (prevPoint >= 0)
					{
						prevPt = new Point2D.Double(ni.getAnchorCenterX() + points[prevPoint].getX(),
							ni.getAnchorCenterY() + points[prevPoint].getY());
						trans.transform(prevPt, prevPt);
						if (prevPt.getX() == thisPt.getX() && prevPt.getY() == thisPt.getY()) prevPoint = -1; else
						{
							Point cPrev = wnd.databaseToScreen(prevPt);
							drawLine(g, wnd, cThis.x + offX, cThis.y + offY, cPrev.x, cPrev.y);
						}
					}
					int nextPoint = point + 1;
					if (nextPoint >= points.length)
					{
						if (showWrap) nextPoint = 0; else
							nextPoint = -1;
					}
					if (nextPoint >= 0)
					{
						nextPt = new Point2D.Double(ni.getAnchorCenterX() + points[nextPoint].getX(),
							ni.getAnchorCenterY() + points[nextPoint].getY());
						trans.transform(nextPt, nextPt);
						if (nextPt.getX() == thisPt.getX() && nextPt.getY() == thisPt.getY()) nextPoint = -1; else
						{
							Point cNext = wnd.databaseToScreen(nextPt);
							drawLine(g, wnd, cThis.x + offX, cThis.y + offY, cNext.x, cNext.y);
						}
					}

					// draw arrows on the lines
					if (offX == 0 && offY == 0 && points.length > 2)
					{
						double arrowLen = Double.MAX_VALUE;
						if (prevPoint >= 0) arrowLen = Math.min(thisPt.distance(prevPt), arrowLen);
						if (nextPoint >= 0) arrowLen = Math.min(thisPt.distance(nextPt), arrowLen);
						arrowLen /= 10;
						if (prevPoint >= 0)
						{
							Point2D prevCtr = new Point2D.Double((prevPt.getX()+thisPt.getX()) / 2,
								(prevPt.getY()+thisPt.getY()) / 2);
							double prevAngle = DBMath.figureAngleRadians(prevPt, thisPt);
							Point2D prevArrow1 = new Point2D.Double(prevCtr.getX() + Math.cos(prevAngle+Math.PI*0.75) * arrowLen,
								prevCtr.getY() + Math.sin(prevAngle+Math.PI*0.75) * arrowLen);
							Point2D prevArrow2 = new Point2D.Double(prevCtr.getX() + Math.cos(prevAngle-Math.PI*0.75) * arrowLen,
								prevCtr.getY() + Math.sin(prevAngle-Math.PI*0.75) * arrowLen);
							Point cPrevCtr = wnd.databaseToScreen(prevCtr);
							Point cPrevArrow1 = wnd.databaseToScreen(prevArrow1);
							Point cPrevArrow2 = wnd.databaseToScreen(prevArrow2);
							drawLine(g, wnd, cPrevCtr.x, cPrevCtr.y, cPrevArrow1.x, cPrevArrow1.y);
							drawLine(g, wnd, cPrevCtr.x, cPrevCtr.y, cPrevArrow2.x, cPrevArrow2.y);
						}

						if (nextPoint >= 0)
						{
							Point2D nextCtr = new Point2D.Double((nextPt.getX()+thisPt.getX()) / 2,
								(nextPt.getY()+thisPt.getY()) / 2);
							double nextAngle = DBMath.figureAngleRadians(thisPt, nextPt);
							Point2D nextArrow1 = new Point2D.Double(nextCtr.getX() + Math.cos(nextAngle+Math.PI*0.75) * arrowLen,
								nextCtr.getY() + Math.sin(nextAngle+Math.PI*0.75) * arrowLen);
							Point2D nextArrow2 = new Point2D.Double(nextCtr.getX() + Math.cos(nextAngle-Math.PI*0.75) * arrowLen,
								nextCtr.getY() + Math.sin(nextAngle-Math.PI*0.75) * arrowLen);
							Point cNextCtr = wnd.databaseToScreen(nextCtr);
							Point cNextArrow1 = wnd.databaseToScreen(nextArrow1);
							Point cNextArrow2 = wnd.databaseToScreen(nextArrow2);
							drawLine(g, wnd, cNextCtr.x, cNextCtr.y, cNextArrow1.x, cNextArrow1.y);
							drawLine(g, wnd, cNextCtr.x, cNextCtr.y, cNextArrow2.x, cNextArrow2.y);
						}
					}

					// do not offset the node, just this point
					offX = offY = 0;
				}
			}

			// draw the selected port
			if (pp != null)
			{
				g.setColor(new Color(User.getColorPortHighlight()));
				Poly poly = ni.getShapeOfPort(pp);
				boolean opened = true;
				if (poly.getStyle() == Poly.Type.FILLED || poly.getStyle() == Poly.Type.CLOSED) opened = false;
				if (poly.getStyle() == Poly.Type.CIRCLE || poly.getStyle() == Poly.Type.THICKCIRCLE ||
					poly.getStyle() == Poly.Type.DISC)
				{
					Point2D [] points = poly.getPoints();
					double sX = points[0].distance(points[1]) * 2;
					Point2D [] pts = Artwork.fillEllipse(points[0], sX, sX, 0, 360);
					drawOutlineFromPoints(wnd, g, pts, offX, offY, opened, null);
				} else if (poly.getStyle() == Poly.Type.CIRCLEARC)
				{
					Point2D [] points = poly.getPoints();
					double [] angles = ni.getArcDegrees();
					double sX = points[0].distance(points[1]) * 2;
					Point2D [] pts = Artwork.fillEllipse(points[0], sX, sX, angles[0], angles[1]);
					drawOutlineFromPoints(wnd, g, pts, offX, offY, opened, null);
				} else
				{
					drawOutlineFromPoints(wnd, g, poly.getPoints(), offX, offY, opened, null);
				}
				g.setColor(new Color(User.getColorHighlight()));

                // show name of port
                if (!(np instanceof PrimitiveNode) && (g instanceof Graphics2D))
				{
					// only show name if port is wired (because all other situations already show the port)
					boolean wired = false;
					for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = (Connection)cIt.next();
						if (con.getPortInst().getPortProto() == pp) { wired = true;   break; }
					}
					if (wired)
					{
	                    Font font = new Font(User.getDefaultFont(), Font.PLAIN, (int)(1.5*EditWindow.getDefaultFontSize()));
    	                GlyphVector v = wnd.getGlyphs(pp.getName(), font);
        	            Point2D point = wnd.databaseToScreen(poly.getCenterX(), poly.getCenterY());
            	        ((Graphics2D)g).drawGlyphVector(v, (float)point.getX()+offX, (float)point.getY()+offY);
					}
                }

				// highlight objects that are electrically connected to this object
                // unless specified not to. HighlightConnected is set to false by addNetwork when
                // it figures out what's connected and adds them manually. Because they are added
                // in addNetwork, we shouldn't try and add connected objects here.
                if (highlightConnected) {
                    Netlist netlist = cell.getUserNetlist();
                    Nodable no = Netlist.getNodableFor(ni, 0);
                    PortProto epp = pp.getEquivalent();
                    if (epp == null) epp = pp;
                    int busWidth = pp.getNameKey().busWidth();

                    FlagSet markObj = Geometric.getFlagSet(1);
                    for(Iterator it = cell.getNodes(); it.hasNext(); )
                        ((NodeInst)it.next()).clearBit(markObj);
                    for(Iterator it = cell.getArcs(); it.hasNext(); )
                    {
                        ArcInst ai = (ArcInst)it.next();
                        ai.clearBit(markObj);

                        if (!netlist.sameNetwork(no, epp, ai)) continue;

                        ai.setBit(markObj);
                        ai.getHead().getPortInst().getNodeInst().setBit(markObj);
                        ai.getTail().getPortInst().getNodeInst().setBit(markObj);
                    }

                    // draw lines along all of the arcs on the network
                    Graphics2D g2 = (Graphics2D)g;
                    g2.setStroke(dashedLine);
                    for(Iterator it = cell.getArcs(); it.hasNext(); )
                    {
                        ArcInst ai = (ArcInst)it.next();
                        if (!ai.isBit(markObj)) continue;
                        Point c1 = wnd.databaseToScreen(ai.getHead().getLocation());
                        Point c2 = wnd.databaseToScreen(ai.getTail().getLocation());
                        drawLine(g, wnd, c1.x, c1.y, c2.x, c2.y);
                    }

                    // draw dots in all connected nodes
                    for(Iterator it = cell.getNodes(); it.hasNext(); )
                    {
                        NodeInst oNi = (NodeInst)it.next();
                        if (oNi == ni) continue;
                        if (!oNi.isBit(markObj)) continue;

                        Point c = wnd.databaseToScreen(oNi.getTrueCenter());
                        g.fillOval(c.x-4, c.y-4, 8, 8);

                        // connect the center dots to the input arcs
                        Point2D nodeCenter = oNi.getTrueCenter();
                        for(Iterator pIt = oNi.getConnections(); pIt.hasNext(); )
                        {
                            Connection con = (Connection)pIt.next();
                            ArcInst ai = con.getArc();
                            if (!ai.isBit(markObj)) continue;
                            Point2D arcEnd = con.getLocation();
                            if (arcEnd.getX() != nodeCenter.getX() || arcEnd.getY() != nodeCenter.getY())
                            {
                                Point c1 = wnd.databaseToScreen(arcEnd);
                                Point c2 = wnd.databaseToScreen(nodeCenter);
                                g2.setStroke(dottedLine);
                                drawLine(g, wnd, c1.x, c1.y, c2.x, c2.y);
                            }
                        }
                    }
                    g2.setStroke(solidLine);
                    markObj.freeFlagSet();
                }
			}
		}
	}

    /**
	 * Returns a printable version of this Highlight.
	 * @return a printable version of this Highlight.
	 */
	public String toString() { return "Highlight "+type; }

	// ************************************* SUPPORT *************************************

    /**
     * Gets a poly that describes the Highlight for the NodeInst.
     * @param ni the nodeinst to get a poly that will be used to highlight it
     * @return a poly outlining the nodeInst.
     */
    public static Poly getNodeInstOutline(NodeInst ni) {

        AffineTransform trans = ni.rotateOutAboutTrueCenter();
        NodeProto np = ni.getProto();

        Poly poly = null;
        if (np instanceof PrimitiveNode)
        {
            // special case for outline nodes
            if (np.isHoldsOutline())
            {
                Point2D [] outline = ni.getTrace();
                if (outline != null)
                {
                    int numPoints = outline.length;
                    Point2D [] pointList = new Point2D.Double[numPoints];
                    for(int i=0; i<numPoints; i++)
                    {
                        pointList[i] = new Point2D.Double(ni.getTrueCenterX() + outline[i].getX(),
                            ni.getTrueCenterY() + outline[i].getY());
                    }
                    trans.transform(pointList, 0, pointList, 0, numPoints);
                    //drawOutlineFromPoints(wnd, g, pointList, 0, 0, true, null);
                    poly = new Poly(pointList);
                    poly.setStyle(Poly.Type.OPENED);
                }
            }
        }

        // setup outline of node with standard offset
        if (poly == null)
        {
            SizeOffset so = ni.getSizeOffset();
            double nodeLowX = ni.getTrueCenterX() - ni.getXSize()/2 + so.getLowXOffset();
            double nodeHighX = ni.getTrueCenterX() + ni.getXSize()/2 - so.getHighXOffset();
            double nodeLowY = ni.getTrueCenterY() - ni.getYSize()/2 + so.getLowYOffset();
            double nodeHighY = ni.getTrueCenterY() + ni.getYSize()/2 - so.getHighYOffset();
            if (nodeLowX == nodeHighX && nodeLowY == nodeHighY)
            {
                float x = (float)nodeLowX;
                float y = (float)nodeLowY;
                Point2D [] outline = new Point2D[1];
                outline[0] = new Point2D.Double(x, y);
                poly = new Poly(outline);
            } else
            {
                double nodeX = (nodeLowX + nodeHighX) / 2;
                double nodeY = (nodeLowY + nodeHighY) / 2;
                poly = new Poly(nodeX, nodeY, nodeHighX-nodeLowX, nodeHighY-nodeLowY);
                poly.transform(trans);
            }
        }

        return poly;
    }

    /**
	 * Method to draw an array of points as highlighting.
	 * @param wnd the window in which drawing is happening.
	 * @param g the Graphics for the window.
	 * @param points the array of points being drawn.
	 * @param offX the X offset of the drawing.
	 * @param offY the Y offset of the drawing.
	 * @param opened true if the points are drawn "opened".
	 * False to close the polygon.
	 */
	private static void drawOutlineFromPoints(EditWindow wnd, Graphics g, Point2D [] points, int offX, int offY, boolean opened, Point2D thickCenter)
	{
        Dimension screen = wnd.getScreenSize();
		boolean onePoint = true;
		Point firstP = wnd.databaseToScreen(points[0].getX(), points[0].getY());
		for(int i=1; i<points.length; i++)
		{
			Point p = wnd.databaseToScreen(points[i].getX(), points[i].getY());
			if (DBMath.doublesEqual(p.getX(), firstP.getX()) &&
				DBMath.doublesEqual(p.getY(), firstP.getY())) continue;
			onePoint = false;
			break;
		}
		if (onePoint)
		{
			drawLine(g, wnd, firstP.x + offX-CROSSSIZE, firstP.y + offY, firstP.x + offX+CROSSSIZE, firstP.y + offY);
			drawLine(g, wnd, firstP.x + offX, firstP.y + offY-CROSSSIZE, firstP.x + offX, firstP.y + offY+CROSSSIZE);
			return;
		}

		// find the center
		int cX = 0, cY = 0;
		if (thickCenter != null)
		{
			Point lp = wnd.databaseToScreen(thickCenter.getX(), thickCenter.getY());
			cX = lp.x;
			cY = lp.y;
		}

		for(int i=0; i<points.length; i++)
		{
			int lastI = i-1;
			if (lastI < 0)
			{
				if (opened) continue;
				lastI = points.length - 1;
			}
			Point lp = wnd.databaseToScreen(points[lastI].getX(), points[lastI].getY());
			Point p = wnd.databaseToScreen(points[i].getX(), points[i].getY());
			int fX = lp.x + offX;   int fY = lp.y + offY;
			int tX = p.x + offX;    int tY = p.y + offY;
			drawLine(g, wnd, fX, fY, tX, tY);
			if (thickCenter != null)
			{
				if (fX < cX) fX--; else fX++;
				if (fY < cY) fY--; else fY++;
				if (tX < cX) tX--; else tX++;
				if (tY < cY) tY--; else tY++;
				drawLine(g, wnd, fX, fY, tX, tY);
			}
		}
	}

    /**
     * Implementing clipping here speeds things up a lot if there are
     * many large highlights off-screen
     */ 
    private static void drawLine(Graphics g, EditWindow wnd, int x1, int y1, int x2, int y2)
    {
        Dimension size = wnd.getScreenSize();
        if (((x1 >= 0) && (x1 <= size.getWidth())) || ((x2 >= 0) && (x2 <= size.getWidth())) ||
            ((y1 >= 0) && (y1 <= size.getHeight())) || ((y2 >= 0) && (y2 <= size.getHeight()))) {
                g.drawLine(x1, y1, x2, y2);
        }
    }

	/**
	 * Method to tell whether two Highlights are the same.
	 * @param other the Highlight to compare to this one.
	 * @return true if the two refer to the same thing.
	 */
	public boolean sameThing(Highlight other)
	{
		if (type != other.getType()) return false;
		if (type == Type.BBOX || type == Type.LINE || type == Type.THICKLINE || type == Type.POLY) return false;

		if (type == Type.EOBJ)
		{
/*
			ElectricObject realEObj = eobj;
			if (realEObj instanceof PortInst) realEObj = ((PortInst)realEObj).getNodeInst();
			ElectricObject realOtherEObj = other.getElectricObject();
			if (realOtherEObj instanceof PortInst) realOtherEObj = ((PortInst)realOtherEObj).getNodeInst();
			if (realEObj != realOtherEObj) return false;
*/
            if (eobj != other.getElectricObject()) return false;
		} else if (type == Type.TEXT)
		{
			if (eobj != other.getElectricObject()) return false;
			if (cell != other.getCell()) return false;
			if (var != other.getVar()) return false;
			if (name != other.getName()) return false;
		}
		return true;
	}

}
