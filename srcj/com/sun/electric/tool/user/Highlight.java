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

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.UserMenuCommands;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ui.EditWindow;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import javax.swing.JPanel;

/*
 * Class for highlighting of objects on the display.
 * <P>
 * These are the types of highlighting that can occur:
 * <UL>
 * <LI>GEOM: a Geometric is selected.  Fills in "geom" and "cell".  Also:
 *   <UL>
 *   <LI>If a NodeInst is selected, may fill-in "port" if a port on that node is selected.
 *   <LI>If a NodeInst is selected, may fill-in "point" if an outline node being edited.
 *   </UL>
 * <LI>TEXT: text is selected.  Fills in "cell", "bounds", and "textStyle".  Also:
 *   <UL>
 *   <LI>For variable on NodeInst or ArcInst, fills in "var" and "geom".
 *   <LI>For variable on a Port, fills in "var", "geom", and "port".
 *   <LI>For Export name, fills in "geom" and "port".
 *   <LI>For variable on Cell, fills in "var".
 *   <LI>For a Cell instance name, fills in "geom".
 *   </UL>
 * <LI>BBOX: a rectangular area is selected.  Fills in "bounds".
 * <LI>LINE: a line is selected.  Fills in "pt1" and "pt2"
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

		private Type(String name) { this.name = name; }

		/**
		 * Returns a printable version of this Type.
		 * @return a printable version of this Type.
		 */
		public String toString() { return name; }

		/** Describes a highlighted geometric. */				public static final Type GEOM = new Type("geometric");
		/** Describes highlighted text. */						public static final Type TEXT = new Type("text");
		/** Describes a highlighted area. */					public static final Type BBOX = new Type("area");
		/** Describes a highlighted line. */					public static final Type LINE = new Type("line");
	}

	/** The type of the highlighting. */						private Type type;
	/** The highlighted object. */								private Geometric geom;
	/** The Cell containing the selection. */					private Cell cell;
	/** The highlighted port (only for NodeInst). */			private PortProto port;
	/** The highlighted outline point (only for NodeInst). */	private int point;
	/** The highlighted variable. */							private Variable var;
	/** The highlighted area. */								private Rectangle2D.Double bounds;
	/** The highlighted line. */								private Point2D.Double pt1, pt2;
	/** The style of highlighted text. */						private Poly.Type textStyle;

	/** Screen offset for display of highlighting. */			private static int highOffX, highOffY;
	/** the highlighted objects. */								private static List highlightList = new ArrayList();

	private Highlight(Type type) { this.type = type; }

	/**
	 * Routine to clear the list of highlighted objects.
	 */
	public static void clearHighlighting()
	{
		highlightList.clear();
		highOffX = highOffY = 0;
	}

	/**
	 * Routine to add a Geometric to the list of highlighted objects.
	 * @param geom the Geometric to add to the list of highlighted objects.
	 * @return the newly created Highlight object.
	 */
	public static Highlight addGeometric(Geometric geom)
	{
		Highlight h = new Highlight(Type.GEOM);
		h.geom = geom;
		h.cell = geom.getParent();

		highlightList.add(h);
		return h;
	}

	private boolean sameThing(Highlight other)
	{
		if (type != other.getType()) return false;
		if (type == Type.BBOX || type == type.LINE) return false;
		if (geom != other.getGeom()) return false;
		if (type == Type.TEXT)
		{
			if (var != other.getVar()) return false;
			if (cell != other.getCell()) return false;
			if (geom != other.getGeom()) return false;
			if (port != other.getPort()) return false;
		}
		return true;
	}
	/**
	 * Routine to return the type of this Highlight (GEOM, TEXT, BBOX, or LINE).
	 * @return the type of this Highlight.
	 */
	public Type getType() { return type; }

	/**
	 * Routine to return the Geometric associated with this Highlight object.
	 * @return the Geometric associated with this Highlight object.
	 */
	public Geometric getGeom() { return geom; }

	/**
	 * Routine to set the Geometric associated with this Highlight object.
	 * @param geom the Geometric associated with this Highlight object.
	 */
	private void setGeom(Geometric geom) { this.geom = geom;   this.cell = geom.getParent(); }

	/**
	 * Routine to return the Cell associated with this Highlight object.
	 * @return the Cell associated with this Highlight object.
	 */
	public Cell getCell() { return cell; }

	/**
	 * Routine to set the Cell associated with this Highlight object.
	 * @param cell the Cell associated with this Highlight object.
	 */
	private void setCell(Cell cell) { this.cell = cell; }

	/**
	 * Routine to set a PortProto to be displayed with this Highlight.
	 * @param port the PortProto to show with this Highlight (must be a NodeInst highlight).
	 */
	public void setPort(PortProto port) { this.port = port; }

	/**
	 * Routine to return the PortProto associated with this Highlight object.
	 * @return the PortProto associated with this Highlight object.
	 */
	public PortProto getPort() { return port; }

	/**
	 * Routine to set an outline point to be displayed with this Highlight.
	 * @param point the outline point to show with this Highlight (must be a NodeInst highlight).
	 */
	public void setPoint(int point) { this.point = point; }

	/**
	 * Routine to return the outline point associated with this Highlight object.
	 * @return the outline point associated with this Highlight object.
	 */
	public int getPoint() { return point; }

	/**
	 * Routine to set an bounds to be displayed with this Highlight.
	 * Bounds are used for area definitions and also for text.
	 * @param point the bounds to show with this Highlight (must be a NodeInst highlight).
	 */
	public void setBounds(Rectangle2D.Double bounds) { this.bounds = bounds; }

	/**
	 * Routine to return the bounds associated with this Highlight object.
	 * Bounds are used for area definitions and also for text.
	 * @return the bounds associated with this Highlight object.
	 */
	public Rectangle2D.Double getBounds() { return bounds; }

	/**
	 * Routine to set an text style to be displayed with this Highlight.
	 * @param point the text style to show with this Highlight (must be a NodeInst highlight).
	 */
	public void setTextStyle(Poly.Type textStyle) { this.textStyle = textStyle; }

	/**
	 * Routine to return the text style associated with this Highlight object.
	 * @return the text style associated with this Highlight object.
	 */
	public Poly.Type getTextStyle() { return textStyle; }

	/**
	 * Routine to return the Variable associated with this Highlight object.
	 * @return the Variable associated with this Highlight object.
	 */
	public Variable getVar() { return var; }

	/**
	 * Routine to return the number of highlighted objects.
	 * @return the number of highlighted objects.
	 */
	public static int getNumHighlights() { return highlightList.size(); }

	/**
	 * Routine to return an Iterator over the highlighted objects.
	 * @return an Iterator over the highlighted objects.
	 */
	public static Iterator getHighlights() { return highlightList.iterator(); }

	/**
	 * Routine to set a screen offset for the display of highlighting.
	 * @param offX the X offset (in pixels) of the highlighting.
	 * @param offY the Y offset (in pixels) of the highlighting.
	 */
	public static void setHighlightOffset(int offX, int offY)
	{
		highOffX = offX;
		highOffY = offY;
	}

	private static final int EXACTSELECTDISTANCE = 5;
	private static final double FARTEXTLIMIT = 20;

	/**
	 * Routine to add everything in an area to the selection.
	 * @param wnd the window being examined.
	 * @param minSelX the low X coordinate of the area.
	 * @param maxSelX the high X coordinate of the area.
	 * @param minSelY the low Y coordinate of the area.
	 * @param maxSelY the high Y coordinate of the area.
	 */
	public static void selectArea(EditWindow wnd, double minSelX, double maxSelX, double minSelY, double maxSelY)
	{
		clearHighlighting();
		Point2D.Double start = wnd.screenToDatabase((int)minSelX-EXACTSELECTDISTANCE, (int)minSelY-EXACTSELECTDISTANCE);
		Point2D.Double end = wnd.screenToDatabase((int)maxSelX+EXACTSELECTDISTANCE, (int)maxSelY+EXACTSELECTDISTANCE);
		Rectangle2D.Double searchArea = new Rectangle2D.Double(Math.min(start.getX(), end.getX()),
			Math.min(start.getY(), end.getY()), Math.abs(start.getX() - end.getX()), Math.abs(start.getY() - end.getY()));
		Geometric.Search sea = new Geometric.Search(searchArea, wnd.getCell());
		for(;;)
		{
			Geometric nextGeom = sea.nextObject();
			if (nextGeom == null) break;
			Highlight h = checkOutObject(nextGeom, false, false, searchArea, wnd);
			if (h != null) highlightList.add(h);
		}
	}

	public static boolean overHighlighted(EditWindow wnd, int oldx, int oldy)
	{
		int numHighlights = getNumHighlights();
		if (numHighlights == 0) return false;

		Point2D.Double slop = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE*2, EXACTSELECTDISTANCE*2);
		double slopWidth = Math.abs(slop.getX());
		double slopHeight = Math.abs(slop.getY());
		Point2D.Double start = wnd.screenToDatabase((int)oldx, (int)oldy);
		Rectangle2D.Double searchArea = new Rectangle2D.Double(start.getX()-slopWidth/2, start.getY()-slopHeight/2, slopWidth, slopHeight);
		Geometric.Search sea = new Geometric.Search(searchArea, wnd.getCell());
		for(;;)
		{
			Geometric nextGeom = sea.nextObject();
			if (nextGeom == null) break;
			Highlight h = checkOutObject(nextGeom, true, false, searchArea, wnd);
			if (h == null) continue;
			for(Iterator it = getHighlights(); it.hasNext(); )
			{
				Highlight alreadyHighlighted = (Highlight)it.next();
				if (alreadyHighlighted.getType() != h.getType()) continue;
				if (alreadyHighlighted.getGeom() == h.getGeom())
				{
					// found it: adjust the port/point
					alreadyHighlighted.setPort(h.getPort());
					alreadyHighlighted.setPoint(h.getPoint());
					return true;
				}
			}
		}
		return false;	
	}

	/**
	 * Routine to display this Highlight in a window.
	 * @param wnd the window in which to draw the highlight.
	 * @param g the Graphics associated with the window.
	 */
	public void showHighlight(EditWindow wnd, Graphics g)
	{
		g.setColor(Color.white);
		if (type == Type.BBOX)
		{
			System.out.println("Highlight BBOX");
			return;
		}
		if (type == Type.LINE)
		{
			System.out.println("Highlight LINE");
			return;
		}
		if (type == Type.TEXT)
		{
			Poly.Type style = getTextStyle();
			Rectangle2D.Double bounds = getBounds();
			Point2D.Double [] points = new Point2D.Double[2];
			if (style == Poly.Type.TEXTCENT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
			} else if (style == Poly.Type.TEXTBOT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
			} else if (style == Poly.Type.TEXTTOP)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
			} else if (style == Poly.Type.TEXTLEFT)
			{
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
			} else if (style == Poly.Type.TEXTRIGHT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
			} else if (style == Poly.Type.TEXTTOPLEFT)
			{
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
			} else if (style == Poly.Type.TEXTBOTLEFT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
			} else if (style == Poly.Type.TEXTTOPRIGHT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
			} else if (style == Poly.Type.TEXTBOTRIGHT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
			}
			return;
		}

		// highlight GEOM
		if (geom instanceof ArcInst)
		{
			// get information about the arc
			ArcInst ai = (ArcInst)geom;
			double offset = ai.getProto().getWidthOffset();

			// construct the polygons that describe the basic arc
			Poly poly = ai.makePoly(ai.getXSize(), ai.getWidth() - offset, Poly.Type.CLOSED);
			if (poly == null) return;
			drawOutlineFromPoints(wnd, g,  poly.getPoints(), highOffX, highOffY);
			return;
		}

		// highlight a NodeInst
		NodeInst ni = (NodeInst)geom;
		NodeProto np = ni.getProto();
		AffineTransform trans = ni.rotateOut();
		SizeOffset so = new SizeOffset(0, 0, 0, 0);
		if (np instanceof PrimitiveNode)
		{
			PrimitiveNode pnp = (PrimitiveNode)np;
			so = Technology.getSizeOffset(ni);

//			// special case for outline nodes
//			int [] specialValues = pnp.getSpecialValues();
//			if (np.isHoldsOutline()) 
//			{
//				Float [] outline = ni.getTrace();
//				if (outline != null)
//				{
//					int numPoints = outline.length / 2;
//					Point2D.Double [] pointList = new Point2D.Double[numPoints];
//					for(int i=0; i<numPoints; i++)
//					{
//						pointList[i] = new Point2D.Double(ni.getCenterX() + outline[i*2].floatValue(),
//							ni.getCenterY() + outline[i*2+1].floatValue());
//					}
//					drawOutlineFromPoints(wnd, g,  pointList, highOffX, highOffY);
//				}
//			}
		}

		// setup outline of node with standard offset
		double portLowX = ni.getCenterX() - ni.getXSize()/2 + so.getLowXOffset();
		double portHighX = ni.getCenterX() + ni.getXSize()/2 - so.getHighXOffset();
		double portLowY = ni.getCenterY() - ni.getYSize()/2 + so.getLowYOffset();
		double portHighY = ni.getCenterY() + ni.getYSize()/2 - so.getHighYOffset();
		if (portLowX == portHighX && portLowY == portHighY)
		{
			float x = (float)portLowX;
			float y = (float)portLowY;
			float size = 3 / (float)wnd.getScale();
			Point c1 = wnd.databaseToScreen(x+size, y);
			Point c2 = wnd.databaseToScreen(x-size, y);
			Point c3 = wnd.databaseToScreen(x, y+size);
			Point c4 = wnd.databaseToScreen(x, y-size);
			g.drawLine(c1.x + highOffX, c1.y + highOffY, c2.x + highOffX, c2.y + highOffY);
			g.drawLine(c3.x + highOffX, c3.y + highOffY, c4.x + highOffX, c4.y + highOffY);
		} else
		{
			double portX = (portLowX + portHighX) / 2;
			double portY = (portLowY + portHighY) / 2;
			Poly poly = new Poly(portX, portY, portHighX-portLowX, portHighY-portLowY);
			poly.transform(trans);
			drawOutlineFromPoints(wnd, g,  poly.getPoints(), highOffX, highOffY);
		}
	
		// draw the selected port
		PortProto pp = getPort();
		if (pp != null)
		{
			PrimitivePort pnp = pp.getBasePort();
			Technology tech = pnp.getParent().getTechnology();
			Poly poly = tech.getShapeOfPort(ni, pnp);
			poly.transform(trans);
			drawOutlineFromPoints(wnd, g,  poly.getPoints(), highOffX, highOffY);
		}
	}

	/**
	 * routine to find an object/port close to (wantx, wanty) in the current cell.
	 * If there is more than one object/port under the cursor, they are returned
	 * in reverse sequential order, provided that the most recently found
	 * object is described in "curhigh".  The next close object is placed in
	 * "curhigh".  If "exclusively" is nonzero, find only nodes or arcs of the
	 * current prototype.  If "another" is nonzero, this is the second find,
	 * and should not consider text objects.  If "findport" is nonzero, port selection
	 * is also desired.  If "under" is nonzero, only find objects exactly under the
	 * desired cursor location.  If "special" is nonzero, special selection rules apply.
	 */
	public static void findObject(Point2D.Double pt, EditWindow wnd, boolean exclusively, boolean another, boolean findPort, boolean under, boolean findSpecial)
	{
		// initialize
		double bestdist = Double.MAX_VALUE;
		boolean looping = false;
		
		// search the relevant objects in the circuit
		Cell cell = wnd.getCell();
		List underCursor = findAllAtPoint(cell, exclusively, another, findPort,
			under, findSpecial, pt, wnd);
//			us_fartextsearch(np, exclusively, another, findport,
//				under, special, curhigh, &best, &bestdirect, &lastdirect, &prevdirect, &looping,
//					&bestdist, wantx, wanty, win, phase);

//		// check for displayable variables on the cell
//		tot = tech_displayablecellvars(np, win, &tech_oneprocpolyloop);
//		for(i=0; i<tot; i++)
//		{
//			var = tech_filldisplayablecellvar(np, poly, win, &varnoeval, &tech_oneprocpolyloop);
//
//			// cell variables are offset from (0,0)
//			us_maketextpoly(poly->string, win, 0, 0, NONODEINST, np->tech,
//				var->textdescript, poly);
//			poly->style = FILLED;
//			dist = polydistance(poly, wantx, wanty);
//			if (dist < 0)
//			{
//				if ((curhigh->status&HIGHTYPE) == HIGHTEXT && curhigh->fromgeom == NOGEOM &&
//					(curhigh->fromvar == var || curhigh->fromport == NOPORTPROTO))
//				{
//					looping = 1;
//					prevdirect.status = lastdirect.status;
//					prevdirect.fromgeom = lastdirect.fromgeom;
//					prevdirect.fromvar = lastdirect.fromvar;
//					prevdirect.fromvarnoeval = lastdirect.fromvarnoeval;
//					prevdirect.fromport = lastdirect.fromport;
//				}
//				lastdirect.status = HIGHTEXT;
//				lastdirect.fromgeom = NOGEOM;
//				lastdirect.fromport = NOPORTPROTO;
//				lastdirect.fromvar = var;
//				lastdirect.fromvarnoeval = varnoeval;
//				if (dist < bestdist)
//				{
//					bestdirect.status = HIGHTEXT;
//					bestdirect.fromgeom = NOGEOM;
//					bestdirect.fromvar = var;
//					bestdirect.fromvarnoeval = varnoeval;
//					bestdirect.fromport = NOPORTPROTO;
//				}
//			}
//
//			// see if it is closer than others
//			if (dist < bestdist)
//			{
//				best.status = HIGHTEXT;
//				best.fromgeom = NOGEOM;
//				best.fromvar = var;
//				best.fromvarnoeval = varnoeval;
//				best.fromport = NOPORTPROTO;
//				bestdist = dist;
//			}
//		}

		// if nothing under the cursor, stop now
		if (underCursor.size() == 0)
		{
			clearHighlighting();
			return;
		}

		// multiple objects under the cursor: see if looping through them
		if (underCursor.size() > 1 && another && highlightList.size() == 1)
		{
			Highlight oldHigh = (Highlight)highlightList.get(0);
			for(int i=0; i<underCursor.size(); i++)
			{
				if (oldHigh.sameThing((Highlight)underCursor.get(i)))
				{
					// found the same thing: loop
					clearHighlighting();
					if (i < underCursor.size()-1)
					{
						highlightList.add(underCursor.get(i+1));
					} else
					{
						highlightList.add(underCursor.get(0));
					}
					return;
				}
			}
		}

		// just use the first in the list
		clearHighlighting();
		highlightList.add(underCursor.get(0));

//		// reevaluate if this is code
//		if ((curhigh->status&HIGHTYPE) == HIGHTEXT && curhigh->fromvar != NOVARIABLE &&
//			curhigh->fromvarnoeval != NOVARIABLE &&
//				curhigh->fromvar != curhigh->fromvarnoeval)
//					curhigh->fromvar = evalvar(curhigh->fromvarnoeval, 0, 0);
	}

	/*
	 * routine to search cell "np" for "far text" objects that are close to (wantx, wanty)
	 * in window "win".  Those that are found are passed to "us_checkoutobject"
	 * for proximity evaluation, along with the evaluation parameters "curhigh",
	 * "best", "bestdirect", "lastdirect", "prevdirect", "looping", "bestdist",
	 * "exclusively", "another", "findport", and "under".  The "phase" value ranges
	 * from 0 to 2 according to the type of object desired.
	 */
//	void us_fartextsearch(NODEPROTO *np, INTBIG exclusively, INTBIG another, INTBIG findport,
//		INTBIG under, INTBIG findspecial, HIGHLIGHT *curhigh, HIGHLIGHT *best, HIGHLIGHT *bestdirect,
//		HIGHLIGHT *lastdirect, HIGHLIGHT *prevdirect, INTBIG *looping, INTBIG *bestdist,
//		INTBIG wantx, INTBIG wanty, WINDOWPART *win, INTBIG phase)
//	{
//		REGISTER NODEINST *ni;
//		REGISTER ARCINST *ai;
//
//		switch (phase)
//		{
//			case 0:			// only allow complex nodes
//				for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//				{
//					if ((ni->userbits&NHASFARTEXT) == 0) continue;
//					if (ni->proto->primindex != 0) continue;
//					us_checkoutobject(ni->geom, 1, exclusively, another, findport, findspecial,
//						curhigh, best, bestdirect, lastdirect, prevdirect, looping,
//							bestdist, wantx, wanty, win);
//				}
//				break;
//			case 1:			// only allow arcs
//				for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//				{
//					if ((ai->userbits&AHASFARTEXT) == 0) continue;
//					us_checkoutobject(ai->geom, 1, exclusively, another, findport, findspecial,
//						curhigh, best, bestdirect, lastdirect, prevdirect, looping,
//							bestdist, wantx, wanty, win);
//				}
//				break;
//			case 2:			// only allow primitive nodes
//				for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//				{
//					if ((ni->userbits&NHASFARTEXT) == 0) continue;
//					if (ni->proto->primindex == 0) continue;
//					us_checkoutobject(ni->geom, 1, exclusively, another, findport, findspecial,
//						curhigh, best, bestdirect, lastdirect, prevdirect, looping,
//							bestdist, wantx, wanty, win);
//				}
//				break;
//		}
//	}

	/**
	 * Returns a printable version of this Highlight.
	 * @return a printable version of this Highlight.
	 */
	public String toString() { return "Highlight "+type; }

	// ************************************* SUPPORT *************************************

	/**
	 * routine to search a Cell for objects that are close to (wantx, wanty).
	 * Those that are found are passed to "us_checkoutobject"
	 * for proximity evaluation, along with the evaluation parameters "curhigh",
	 * "best", "bestdirect", "lastdirect", "prevdirect", "looping", "bestdist",
	 * "exclusively", "another", "findport", and "under".  The "phase" value ranges
	 * from 0 to 2 according to the type of object desired.
	 */
	private static List findAllAtPoint(Cell cell, boolean exclusively, boolean another, boolean findPort,
		boolean under, boolean findSpecial, Point2D.Double pt, EditWindow wnd)
	{
		// make a list of things under the cursor
		List list = new ArrayList();

		Rectangle2D.Double bounds = new Rectangle2D.Double(pt.getX(), pt.getY(), 0, 0);

		// determine proper area to search
		double slop = FARTEXTLIMIT;
		Point2D.Double extra = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE, EXACTSELECTDISTANCE);
		double directHitDist = extra.getX();
		if (directHitDist > slop) slop = directHitDist;
		Rectangle2D.Double searchArea = new Rectangle2D.Double(pt.getX() - slop, pt.getY() - slop, slop*2, slop*2);

		// 4 phases of examination: text, cells, arcs, then primitive nodes
		for(int phase=0; phase<4; phase++)
		{
			// ignore cells if requested
//			if (phase == 1 && !findSpecial && (us_useroptions&NOINSTANCESELECT) != 0) continue;

			// examine everything in the area
			Geometric.Search sea = new Geometric.Search(searchArea, wnd.getCell());
			for(;;)
			{
				Geometric geom = sea.nextObject();
				if (geom == null) break;

				Highlight h;
				switch (phase)
				{
					case 0:
						// check text
						Poly [] polys = null;
						if (geom instanceof NodeInst)
						{
							NodeInst ni = (NodeInst)geom;
							polys = ni.getAllText(findSpecial, wnd);
						} else
						{
							ArcInst ai = (ArcInst)geom;
							polys = ai.getAllText(findSpecial, wnd);
						}
						if (polys == null) break;
						for(int i=0; i<polys.length; i++)
						{
							Poly poly = polys[i];
							poly.setExactTextBounds(wnd);
							double dist = poly.polyDistance(bounds);
							if (dist >= directHitDist) continue;
							h = new Highlight(Type.TEXT);
							if (h != null)
							{
								h.setCell(cell);
								h.setTextStyle(poly.getStyle());
								h.setBounds(poly.getBounds2DDouble());
								list.add(h);
							}
						}
					case 1:			// check Cell instances
						if (geom instanceof ArcInst) break;
						if (((NodeInst)geom).getProto() instanceof PrimitiveNode) break;
						h = checkOutObject(geom, findPort, findSpecial, bounds, wnd);
						if (h != null) list.add(h);
						break;
					case 2:			// check arcs
						if (geom instanceof NodeInst) break;
						h = checkOutObject(geom, findPort, findSpecial, bounds, wnd);
						if (h != null) list.add(h);
						break;
					case 3:			// check primitive nodes
						if (geom instanceof ArcInst) break;
						if (((NodeInst)geom).getProto() instanceof Cell) break;
						h = checkOutObject(geom, findPort, findSpecial, bounds, wnd);
						if (h != null) list.add(h);
						break;
				}
			}
		}
		return list;
	}

	/**
	 * Routine to determine whether an object is selected.
	 * @param geom the Geometric being tested for selection.
	 * @param findPort true if a port should be selected with a NodeInst.
	 * @param findSpecial true if hard-to-select and other special selection is being done.
	 * @param bounds the selected area or point.
	 * @param wnd the window being examined
	 * @return a Highlight that defines the object, or null if the point is not over any part of this object.
	 */
	private static Highlight checkOutObject(Geometric geom, boolean findPort, boolean findSpecial, Rectangle2D.Double bounds, EditWindow wnd)
	{
		// compute threshold for direct hits
		Point2D.Double extra = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE, EXACTSELECTDISTANCE);
		double directHitDist = extra.getX();

		if (geom instanceof NodeInst)
		{
			// examine a node object
			NodeInst ni = (NodeInst)geom;

			// do not "find" hard-to-find nodes if "findSpecial" is not set
			if (!findSpecial && ni.isHardSelect()) return null;

			// do not include primitives that have all layers invisible
//			if (ni.getProto() instanceof PrimitiveNode && (ni->proto->userbits&NINVISIBLE) != 0) return;

			// do not "find" Invisible-Pins if they have text or exports
			if (ni.getProto() == Generic.tech.invisiblePin_node)
			{
				if (ni.getNumExports() != 0) return null;
				if (ni.numDisplayableVariables(false) != 0) return null;
			}

			// get the distance to the object
			double dist = distToNode(bounds, ni, wnd);

			// direct hit
			if (dist < directHitDist)
			{
				Highlight h = new Highlight(Type.GEOM);
				h.setGeom(geom);

				// add the closest port
				if (findPort)
				{
					AffineTransform trans = ni.rotateOut();
					double bestDist = Double.MAX_VALUE;
					PortInst bestPort = null;
					for(Iterator it = ni.getPortInsts(); it.hasNext(); )
					{
						PortInst pi = (PortInst)it.next();
						PrimitivePort pp = pi.getPortProto().getBasePort();
						Technology tech = pp.getParent().getTechnology();
						Poly poly = tech.getShapeOfPort(ni, pp);
						poly.transform(trans);
						dist = poly.polyDistance(bounds);
						if (dist < bestDist)
						{
							bestDist = dist;
							bestPort = pi;
						}
					}
					if (bestPort != null) h.setPort(bestPort.getPortProto());
				}
				return h;
			}
		} else
		{
			// examine an arc object
			ArcInst ai = (ArcInst)geom;

			// do not "find" hard-to-find arcs if "findSpecial" is not set
			if (!findSpecial && ai.isHardSelect()) return null;

			// do not include arcs that have all layers invisible
//			if ((ai->proto->userbits&AINVISIBLE) != 0) return;

			// get distance to arc
			double dist = distToArc(bounds, ai, wnd);

			// direct hit
			if (dist < directHitDist)
			{
				Highlight h = new Highlight(Type.GEOM);
				h.setGeom(geom);
				return h;
			}
		}
		return null;
	}

	/**
	 * Routine to return the distance from a bound to a NodeInst.
	 * @param bounds the bounds in question.
	 * @param ni the NodeInst.
	 * @return the distance from the bounds to the NodeInst.
	 * Negative values are direct hits.
	 */
	private static double distToNode(Rectangle2D.Double bounds, NodeInst ni, EditWindow wnd)
	{
		AffineTransform trans = ni.rotateOut();

		NodeProto np = ni.getProto();
		if (np instanceof PrimitiveNode)
		{
			// special case for MOS transistors: examine the gate/active tabs
			NodeProto.Function fun = np.getFunction();
			if (fun == NodeProto.Function.TRANMOS || fun == NodeProto.Function.TRAPMOS || fun == NodeProto.Function.TRADMOS)
			{
				Technology tech = np.getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, wnd);
				double bestDist = Double.MAX_VALUE;
				for(int box=0; box<polys.length; box++)
				{
					Poly poly = polys[box];
					Layer layer = poly.getLayer();
					if (layer == null) continue;
					Layer.Function lf = layer.getFunction();
					if (!lf.isPoly() && !lf.isDiff()) continue;
					poly.transform(trans);
					double dist = poly.polyDistance(bounds);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			}

			// special case for 1-polygon primitives: check precise distance to cursor
			if (np.isEdgeSelect())
			{
				Technology tech = np.getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, wnd);
				double bestDist = Double.MAX_VALUE;
				for(int box=0; box<polys.length; box++)
				{
					Poly poly = polys[box];
					poly.transform(trans);
					double dist = poly.polyDistance(bounds);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			}
		}

		// get the bounds of the node in a polygon
		SizeOffset so = ni.getProto().getSizeOffset();
		Rectangle2D.Double niBounds = ni.getBounds();
		double lX = niBounds.getMinX() + so.getLowXOffset();
		double hX = niBounds.getMaxX() + so.getHighXOffset();
		double lY = niBounds.getMinY() + so.getLowYOffset();
		double hY = niBounds.getMaxY() + so.getHighYOffset();
		Poly nodePoly = new Poly((lX+hX)/2, (lY+hY)/2, hX-lX, hY-lY);
		nodePoly.setStyle(Poly.Type.FILLED);
		nodePoly.transform(trans);
		double dist = nodePoly.polyDistance(bounds);
		return dist;
	}

	/**
	 * Routine to return the distance from a bounds to an ArcInst.
	 * @param bounds the bounds in question.
	 * @param ai the ArcInst.
	 * @return the distance from the bounds to the ArcInst.
	 * Negative values are direct hits or intersections.
	 */
	private static double distToArc(Rectangle2D.Double bounds, ArcInst ai, EditWindow wnd)
	{
		ArcProto ap = ai.getProto();

		// if arc is selectable precisely, check distance to cursor
		if (ap.isEdgeSelect())
		{
			Technology tech = ap.getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai, wnd);
			double bestDist = Double.MAX_VALUE;
			for(int box=0; box<polys.length; box++)
			{
				Poly poly = polys[box];
				double dist = poly.polyDistance(bounds);
				if (dist < bestDist) bestDist = dist;
			}
			return bestDist;
		}

		// standard distance to the arc
		double wid = ai.getWidth() - ai.getProto().getWidthOffset();
		if (EMath.doublesEqual(wid, 0)) wid = 1;
//		if (curvedarcoutline(ai, poly, FILLED, wid))
			Poly poly = ai.makePoly(ai.getXSize(), wid, Poly.Type.FILLED);
		return poly.polyDistance(bounds);
	}

	private static final int CROSSSIZE = 3;

	private static void drawOutlineFromPoints(EditWindow wnd, Graphics g, Point2D.Double [] points, int offX, int offY)
	{
		boolean onePoint = true;
		Point firstP = wnd.databaseToScreen(points[0].getX(), points[0].getY());
		for(int i=1; i<points.length; i++)
		{
			Point p = wnd.databaseToScreen(points[i].getX(), points[i].getY());
			if (EMath.doublesEqual(p.getX(), firstP.getX()) &&
				EMath.doublesEqual(p.getY(), firstP.getY())) continue;
			onePoint = false;
			break;
		}
		if (onePoint)
		{
			g.drawLine(firstP.x + offX-CROSSSIZE, firstP.y + offY, firstP.x + offX+CROSSSIZE, firstP.y + offY);
			g.drawLine(firstP.x + offX, firstP.y + offY-CROSSSIZE, firstP.x + offX, firstP.y + offY+CROSSSIZE);
			return;
		}
		for(int i=0; i<points.length; i++)
		{
			int lastI = i-1;
			if (i == 0) lastI = points.length-1;
			Point lp = wnd.databaseToScreen(points[lastI].getX(), points[lastI].getY());
			Point p = wnd.databaseToScreen(points[i].getX(), points[i].getY());
			g.drawLine(lp.x + offX, lp.y + offY, p.x + offX, p.y + offY);
		}
	}

}

