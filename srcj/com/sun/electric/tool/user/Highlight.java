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
import com.sun.electric.database.text.Name;
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
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.UserMenuCommands;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.dialogs.GetInfoNode;
import com.sun.electric.tool.user.dialogs.GetInfoArc;
import com.sun.electric.tool.user.dialogs.GetInfoExport;
import com.sun.electric.tool.user.dialogs.GetInfoText;
import com.sun.electric.tool.user.dialogs.GetInfoMulti;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;

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
 * <LI>TEXT: text is selected.  Fills in "cell".  Also:
 *   <UL>
 *   <LI>For variable on NodeInst or ArcInst, fills in "var" and "geom".
 *   <LI>For the name on NodeInst or ArcInst, fills in "name" and "geom".
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
	/** The highlighted Name. */								private Name name;
	/** The highlighted area. */								private Rectangle2D bounds;
	/** The highlighted line. */								private Point2D pt1, pt2;

	/** Screen offset for display of highlighting. */			private static int highOffX, highOffY;
	/** the highlighted objects. */								private static List highlightList = new ArrayList();

	private static final int EXACTSELECTDISTANCE = 5;
	private static final int CROSSSIZE = 3;

	private Highlight(Type type)
	{
		this.type = type;
		this.geom = null;
		this.cell = null;
		this.port = null;
		this.point = 0;
		this.var = null;
		this.name = null;
		this.bounds = null;
		this.pt1 = null;
		this.pt2 = null;
	}

	/**
	 * Routine to clear the list of highlighted objects.
	 */
	public static void clear()
	{
		highlightList.clear();
		highOffX = highOffY = 0;
	}

	/**
	 * Routine to indicate that changes to highlighting are finished.
	 * Call this after any change to highlighting.
	 */
	public static void finished()
	{
		GetInfoNode.load();
		GetInfoArc.load();
		GetInfoExport.load();
		GetInfoText.load();
		GetInfoMulti.load();
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			wf.getEditWindow().repaint();
		}
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

	/**
	 * Routine to add a text selection to the list of highlighted objects.
	 * @param cell the Cell in which this area resides.
	 * @param var the Variable associated with the text (text is then a visual of that variable).
	 * @param name the Name associated with the text (for the name of Nodes and Arcs).
	 * @return the newly created Highlight object.
	 */
	public static Highlight addText(Cell cell, Variable var, Name name)
	{
		Highlight h = new Highlight(Type.TEXT);
		h.cell = cell;
		h.var = var;
		h.name = name;

		highlightList.add(h);
		return h;
	}

	/**
	 * Routine to add an area to the list of highlighted objects.
	 * @param area the Rectangular area to add to the list of highlighted objects.
	 * @param cell the Cell in which this area resides.
	 * @return the newly created Highlight object.
	 */
	public static Highlight addArea(Rectangle2D area, Cell cell)
	{
		Highlight h = new Highlight(Type.BBOX);
		h.bounds = new Rectangle2D.Double();
		h.bounds.setRect(area);
		h.cell = cell;

		highlightList.add(h);
		return h;
	}

	/**
	 * Routine to add a line to the list of highlighted objects.
	 * @param start the start point of the line to add to the list of highlighted objects.
	 * @param end the end point of the line to add to the list of highlighted objects.
	 * @param cell the Cell in which this line resides.
	 * @return the newly created Highlight object.
	 */
	public static Highlight addLine(Point2D start, Point2D end, Cell cell)
	{
		Highlight h = new Highlight(Type.LINE);
		h.pt1 = new Point2D.Double(start.getX(), start.getY());
		h.pt2 = new Point2D.Double(end.getX(), end.getY());
		h.cell = cell;

		highlightList.add(h);
		return h;
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
	public void setGeom(Geometric geom) { this.geom = geom;   this.cell = geom.getParent(); }

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
	 * Routine to return the bounds associated with this Highlight object.
	 * Bounds are used for area definitions and also for text.
	 * @return the bounds associated with this Highlight object.
	 */
	public Rectangle2D getBounds() { return bounds; }

	/**
	 * Routine to set the Variable associated with this Highlight object.
	 * @param var the Variable associated with this Highlight object.
	 */
	private void setVar(Variable var) { this.var = var; }

	/**
	 * Routine to return the Name associated with this Highlight object.
	 * @return the Name associated with this Highlight object.
	 */
	public Name getName() { return name; }

	/**
	 * Routine to set the Name associated with this Highlight object.
	 * @param name the Name associated with this Highlight object.
	 */
	private void setName(Name name) { this.name = name; }

	/**
	 * Routine to return the "from point" associated with this Highlight object.
	 * This only applies to Highlights of type LINE.
	 * @return the from point associated with this Highlight object.
	 */
	public Point2D getFromPoint() { return pt1; }

	/**
	 * Routine to return the "to point" associated with this Highlight object.
	 * This only applies to Highlights of type LINE.
	 * @return the to point associated with this Highlight object.
	 */
	public Point2D getToPoint() { return pt2; }

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
	 * Routine to load a list of Highlights into the highlighting.
	 * @param newHighlights a List of Highlight objects.
	 */
	public static void setHighlightList(List newHighlights)
	{
		for(Iterator it = newHighlights.iterator(); it.hasNext(); )
		{
			highlightList.add(it.next());
		}
	}

	/**
	 * Routine to return an List of all highlighted Geometrics.
	 * @param wantNodes true if NodeInsts should be included in the list.
	 * @param wantArcs true if ArcInsts should be included in the list.
	 * @return a list with the highlighted Geometrics.
	 */
	public static List getHighlighted(boolean wantNodes, boolean wantArcs)
	{
		// now place the objects in the list
		List highlightedGeoms = new ArrayList();
		for(Iterator it = getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();

			if (h.getType() == Type.GEOM)
			{
				Geometric geom = h.getGeom();
				if (geom instanceof NodeInst && !wantNodes) continue;
				if (geom instanceof ArcInst && !wantArcs) continue;

				if (highlightedGeoms.contains(geom)) continue;
				highlightedGeoms.add(geom);
			}
			if (h.getType() == Type.BBOX)
			{
				List inArea = findAllInArea(h.getCell(), false, false, false, false, false, h.getBounds(), null);
				for(Iterator ait = inArea.iterator(); ait.hasNext(); )
				{
					Highlight ah = (Highlight)ait.next();
					if (ah.getType() == Type.GEOM)
						highlightedGeoms.add(ah.getGeom());
				}
			}
		}
		return highlightedGeoms;
	}

	/**
	 * Routine to return the only highlighted object.
	 * If there is not one highlighted object, an error is issued.
	 * @return the highlighted object (null if error).
	 */
	public static Highlight getOneHighlight()
	{
		if (getNumHighlights() == 0)
		{
			System.out.println("Must select an object first");
			return null;
		}
		if (getNumHighlights() > 1)
		{
			System.out.println("Must select only one object");
			return null;
		}
		Highlight h = (Highlight)getHighlights().next();
		return h;
	}

	/**
	 * Routine to return the current window in which the highlights reside.
	 * @return the EditWindow to redraw with the highlighted objects.
	 * Prints an error messagen returns null if no EditWindow can be determined.
	 */
	public static EditWindow getHighlightedWindow()
	{
		EditWindow undisplayedAlternate = null;
		EditWindow curWind = TopLevel.getCurrentEditWindow();
		Library lib = Library.getCurrent();
		Cell cell = lib.getCurCell();
		if (cell != null)
		{
			if (curWind != null && curWind.getCell() == cell) return curWind;

//			!!! find a beter way to determine the CURRENT window !!!
			for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
			{
				WindowFrame wf = (WindowFrame)it.next();
				curWind = wf.getEditWindow();
				if (curWind != null) break;
				if (curWind.getCell() == cell) break;
			}
		}

		if (getNumHighlights() > 0)
		{
			// determine the cell with these geometrics
			for(Iterator it = getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() == Type.GEOM || h.getType() == Type.BBOX || h.getType() == Type.LINE)
				{
					Cell parent = h.getGeom().getParent();
					if (curWind != null && curWind.getCell() == parent) return curWind;
					EditWindow wnd = EditWindow.findWindow(parent);
					if (wnd != null) undisplayedAlternate = wnd;
				}
			}
			if (undisplayedAlternate != null)
				return undisplayedAlternate;
		}

		if (curWind != null) return curWind;

		System.out.println("No current window");
		return null;
	}

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

	/**
	 * Routine to add everything in an area to the selection.
	 * @param wnd the window being examined.
	 * @param minSelX the low X coordinate of the area in database units.
	 * @param maxSelX the high X coordinate of the area in database units.
	 * @param minSelY the low Y coordinate of the area in database units.
	 * @param maxSelY the high Y coordinate of the area in database units.
	 * @param invertSelection is true to invert the selection (remove what is already highlighted and add what is new).
	 * @param findSpecial is true to find hard-to-select objects.
	 */
	public static void selectArea(EditWindow wnd, double minSelX, double maxSelX, double minSelY, double maxSelY,
		boolean invertSelection, boolean findSpecial)
	{
		Rectangle2D searchArea = new Rectangle2D.Double(minSelX, minSelY, maxSelX - minSelX, maxSelY - minSelY);
		List underCursor = findAllInArea(wnd.getCell(), false, false, false, findSpecial, true, searchArea, wnd);
		if (invertSelection)
		{
			for(Iterator it = underCursor.iterator(); it.hasNext(); )
			{
				Highlight newHigh = (Highlight)it.next();
				boolean found = false;
				for(int i=0; i<highlightList.size(); i++)
				{
					Highlight oldHigh = (Highlight)highlightList.get(i);
					if (newHigh.sameThing(oldHigh))
					{
						highlightList.remove(i);
						found = true;
						break;
					}
				}
				if (found) continue;
				highlightList.add(newHigh);
			}
		} else
		{
			setHighlightList(underCursor);
		}
	}

	/**
	 * Routine to tell whether a point is over this Highlight.
	 * @param wnd the window being examined.
	 * @param x the X coordinate of the point.
	 * @param y the Y coordinate of the point.
	 * @return true if the point is over this Highlight.
	 */
	public static boolean overHighlighted(EditWindow wnd, int x, int y)
	{
		int numHighlights = getNumHighlights();
		if (numHighlights == 0) return false;

		Point2D slop = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE*2, EXACTSELECTDISTANCE*2);
		double directHitDist = slop.getX();
		double slopWidth = Math.abs(slop.getX());
		double slopHeight = Math.abs(slop.getY());
		Point2D start = wnd.screenToDatabase((int)x, (int)y);
		Rectangle2D searchArea = new Rectangle2D.Double(start.getX()-slopWidth/2, start.getY()-slopHeight/2, slopWidth, slopHeight);
		Geometric.Search sea = new Geometric.Search(searchArea, wnd.getCell());
		for(;;)
		{
			Geometric nextGeom = sea.nextObject();
			if (nextGeom == null) break;
			Highlight h = checkOutObject(nextGeom, true, true, searchArea, wnd, directHitDist);
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
	 * @param wnd the window in which to draw this highlight.
	 * @param g the Graphics associated with the window.
	 */
	public void showHighlight(EditWindow wnd, Graphics g)
	{
		g.setColor(Color.white);
		if (type == Type.BBOX)
		{
			Point2D [] points = new Point2D.Double[5];
			points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
			points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
			points[2] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
			points[3] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
			points[4] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
			drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
			return;
		}
		if (type == Type.LINE)
		{
			Point2D [] points = new Point2D.Double[2];
			points[0] = new Point2D.Double(pt1.getX(), pt1.getY());
			points[1] = new Point2D.Double(pt2.getX(), pt2.getY());
			drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
//			System.out.println("Highlight LINE");
			return;
		}
		if (type == Type.TEXT)
		{
			Poly poly = computeTextPoly(wnd);
			if (poly == null) return;
			Poly.Type style = poly.getStyle();
			Rectangle2D bounds = poly.getBounds2D();
			Point2D [] points = new Point2D.Double[2];
			if (style == Poly.Type.TEXTCENT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
//				g.drawLine((int)points[0].getX() + highOffX, (int)points[0].getY() + highOffY, (int)points[1].getX() + highOffX, (int)points[1].getY() + highOffY);
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
//				g.drawLine((int)points[0].getX() + highOffX, (int)points[0].getY() + highOffY, (int)points[1].getX() + highOffX, (int)points[1].getY() + highOffY);
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
			} else if (style == Poly.Type.TEXTBOT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
			} else if (style == Poly.Type.TEXTTOP)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
			} else if (style == Poly.Type.TEXTLEFT)
			{
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
			} else if (style == Poly.Type.TEXTRIGHT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
			} else if (style == Poly.Type.TEXTTOPLEFT)
			{
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
			} else if (style == Poly.Type.TEXTBOTLEFT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
			} else if (style == Poly.Type.TEXTTOPRIGHT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
			} else if (style == Poly.Type.TEXTBOTRIGHT)
			{
				points[0] = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
				points[0] = new Point2D.Double(bounds.getMaxX(), bounds.getMinY());
				points[1] = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
				drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY, false);
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
			drawOutlineFromPoints(wnd, g,  poly.getPoints(), highOffX, highOffY, false);

			if (getNumHighlights() == 1)
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
				Point p = wnd.databaseToScreen(ai.getCenterX(), ai.getCenterY());
				GlyphVector gv = wnd.getGlyphs(constraints, null);
				Rectangle2D glyphBounds = gv.getVisualBounds();
				g.drawString(constraints, (int)(p.x - glyphBounds.getWidth()/2 + highOffX),
					(int)(p.y + glyphBounds.getHeight()/2 + highOffY));
			}
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
//					Point2D [] pointList = new Point2D.Double[numPoints];
//					for(int i=0; i<numPoints; i++)
//					{
//						pointList[i] = new Point2D.Double(ni.getCenterX() + outline[i*2].floatValue(),
//							ni.getCenterY() + outline[i*2+1].floatValue());
//					}
//					drawOutlineFromPoints(wnd, g,  pointList, highOffX, highOffY, false);
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
			drawOutlineFromPoints(wnd, g,  poly.getPoints(), highOffX, highOffY, false);
		}

		// draw the selected port
		PortProto pp = getPort();
		if (pp != null)
		{
			Poly poly = ni.getShapeOfPort(pp);
			boolean opened = true;
			if (poly.getStyle() == Poly.Type.FILLED || poly.getStyle() == Poly.Type.CLOSED) opened = false;
			poly.transform(trans);
			if (poly.getStyle() == Poly.Type.CIRCLE || poly.getStyle() == Poly.Type.THICKCIRCLE ||
				poly.getStyle() == Poly.Type.DISC)
			{
				Point2D [] points = poly.getPoints();
				double sX = points[0].distance(points[1]) * 2;
				Point2D [] pts = Artwork.fillEllipse(points[0], sX, sX, 0, 360);
				drawOutlineFromPoints(wnd, g,  pts, highOffX, highOffY, opened);
			} else if (poly.getStyle() == Poly.Type.CIRCLEARC)
			{
				Point2D [] points = poly.getPoints();
				double [] angles = ni.getArcDegrees();
				double sX = points[0].distance(points[1]) * 2;
				Point2D [] pts = Artwork.fillEllipse(points[0], sX, sX, angles[0], angles[1]);
				drawOutlineFromPoints(wnd, g,  pts, highOffX, highOffY, opened);
			} else
			{
				drawOutlineFromPoints(wnd, g,  poly.getPoints(), highOffX, highOffY, opened);
			}
		}
	}

	private Poly computeTextPoly(EditWindow wnd)
	{
		Poly poly = null;
		Variable var = getVar();
		Geometric geom = getGeom();
		if (var != null)
		{
			if (geom != null)
			{
				if (getPort() != null)
				{
					PortInst pi = ((Export)getPort()).getOriginalPort();
					Rectangle2D bounds = pi.getPoly().getBounds2D();
					Poly [] polys = getPort().getPolyList(var, bounds.getCenterX(), bounds.getCenterY(), wnd, false);
					if (polys != null)
					{
						poly = polys[0];
						poly.transform(pi.getNodeInst().rotateOut());
					}
				} else
				{
					Poly [] polys = geom.getPolyList(var, geom.getCenterX(), geom.getCenterY(), wnd, false);
					if (polys != null)
					{
						poly = polys[0];
						if (geom instanceof NodeInst)
							poly.transform(((NodeInst)geom).rotateOut());
					}
				}
			} else
			{
				Rectangle2D bounds = getCell().getBounds();
				Poly [] polys = getCell().getPolyList(var, 0, 0, wnd, false);
				if (polys != null) poly = polys[0];
			}
			if (poly != null)
				poly.setExactTextBounds(wnd);
		} else
		{
			if (geom == null) return null;
			if (getName() != null)
			{
				TextDescriptor td = geom.getNameTextDescriptor();
				Point2D [] pointList = new Point2D.Double[1];
				pointList[0] = new Point2D.Double(geom.getCenterX()+td.getXOff(), geom.getCenterY()+td.getYOff());
				poly = new Poly(pointList);
				poly.setStyle(td.getPos().getPolyType());
				if (geom instanceof NodeInst)
				{
					poly.transform(((NodeInst)geom).rotateOut());
				}
				Name name = getName();
				poly.setTextDescriptor(td);
				poly.setString(name.toString());
				poly.setExactTextBounds(wnd);
			} else
			{
				if (getPort() != null)
				{
					// export name
					Export pp = (Export)getPort();
					Rectangle2D bounds = pp.getOriginalPort().getBounds();
					TextDescriptor td = pp.getTextDescriptor();
					Point2D [] pointList = new Point2D.Double[1];
					pointList[0] = new Point2D.Double(bounds.getCenterX()+td.getXOff(), bounds.getCenterY()+td.getYOff());
					poly = new Poly(pointList);
					poly.setStyle(td.getPos().getPolyType());
					poly.setTextDescriptor(td);
					poly.setString(pp.getProtoName());
					poly.setExactTextBounds(wnd);
				} else
				{
					// cell instance name
					NodeInst ni = (NodeInst)geom;
					TextDescriptor td = ni.getProtoTextDescriptor();
					Point2D [] pointList = new Point2D.Double[1];
					pointList[0] = new Point2D.Double(ni.getCenterX()+td.getXOff(), ni.getCenterY()+td.getYOff());
					poly = new Poly(pointList);
					poly.setStyle(td.getPos().getPolyType());
					poly.transform(ni.rotateOut());
					poly.setTextDescriptor(td);
					poly.setString(ni.getProto().describe());
					poly.setExactTextBounds(wnd);
				}
			}
		}
		return poly;
	}

	/**
	 * Routine to handle a click in a window and select the appropriate objects.
	 * @param pt the coordinates of the click (in database units).
	 * @param wnd the window being examined.
	 * @param exclusively true if the currently selected object must remain selected.
	 * This happens during "outline edit" when the node doesn't change, just the point on it.
	 * @param another true to find another object under the point (when there are multiple ones).
	 * @param invert true to invert selection (add if not selected, remove if already selected).
	 * @param findPort true to also show the closest port on a selected node.
	 * @param findSpecial true to select hard-to-find objects.
	 * @param findText true to select text objects.
	 * The name of an unexpanded cell instance is always hard-to-select.
	 * Other objects are set this way by the user (although the cell-center is usually set this way).
	 */
	public static int findObject(Point2D pt, EditWindow wnd, boolean exclusively,
		boolean another, boolean invert, boolean findPort, boolean findSpecial, boolean findText)
	{
		// initialize
		double bestdist = Double.MAX_VALUE;
		boolean looping = false;
		
		// search the relevant objects in the circuit
		Cell cell = wnd.getCell();
		Rectangle2D bounds = new Rectangle2D.Double(pt.getX(), pt.getY(), 0, 0);
		List underCursor = findAllInArea(cell, exclusively, another, findPort, findSpecial, findText, bounds, wnd);

		// if nothing under the cursor, stop now
		if (underCursor.size() == 0)
		{
			if (!invert)
			{
				clear();
				finished();
			}
			return 0;
		}

		// multiple objects under the cursor: see if looping through them
		if (underCursor.size() > 1 && another)
		{
			for(int j=0; j<highlightList.size(); j++)
			{
				Highlight oldHigh = (Highlight)highlightList.get(j);
				for(int i=0; i<underCursor.size(); i++)
				{
					if (oldHigh.sameThing((Highlight)underCursor.get(i)))
					{
						// found the same thing: loop
						if (invert)
						{
							highlightList.remove(j);
						} else
						{
							clear();
						}
						if (i < underCursor.size()-1)
						{
							highlightList.add(underCursor.get(i+1));
						} else
						{
							highlightList.add(underCursor.get(0));
						}
						finished();
						return 1;
					}
				}
			}
		}

		// just use the first in the list
		if (invert)
		{
			Highlight newHigh = (Highlight)underCursor.get(0);
			for(int i=0; i<highlightList.size(); i++)
			{
				if (newHigh.sameThing((Highlight)highlightList.get(i)))
				{
					highlightList.remove(i);
					finished();
					return 1;
				}
			}
			highlightList.add(newHigh);
			finished();
		} else
		{
			clear();
			highlightList.add(underCursor.get(0));
			finished();
		}

//		// reevaluate if this is code
//		if ((curhigh->status&HIGHTYPE) == HIGHTEXT && curhigh->fromvar != NOVARIABLE &&
//			curhigh->fromvarnoeval != NOVARIABLE &&
//				curhigh->fromvar != curhigh->fromvarnoeval)
//					curhigh->fromvar = evalvar(curhigh->fromvarnoeval, 0, 0);
		return 1;
	}

	/**
	 * Returns a printable version of this Highlight.
	 * @return a printable version of this Highlight.
	 */
	public String toString() { return "Highlight "+type; }

	// ************************************* SUPPORT *************************************

	/**
	 * Routine to search a Cell for all objects at a point.
	 * @param cell the cell to search.
	 * @param exclusively true if the currently selected object must remain selected.
	 * This happens during "outline edit" when the node doesn't change, just the point on it.
	 * @param another true to find another object under the point (when there are multiple ones).
	 * @param findPort true to also show the closest port on a selected node.
	 * @param findSpecial true to select hard-to-find objects.
	 * @param findText true to select text objects.
	 * The name of an unexpanded cell instance is always hard-to-select.
	 * Other objects are set this way by the user (although the cell-center is usually set this way).
	 * @param bounds the area of the search (in database units).
	 * @param wnd the window being examined (null to ignore window scaling).
	 * @return a list of Highlight objects.
	 * The list is ordered by importance, so the deault action is to select the first entry.
	 */
	public static List findAllInArea(Cell cell, boolean exclusively, boolean another, boolean findPort,
		 boolean findSpecial, boolean findText, Rectangle2D bounds, EditWindow wnd)
	{
		// make a list of things under the cursor
		List list = new ArrayList();

		// this is the distance from an object that is necessary for a "direct hit"
		double directHitDist = 0;
		if (wnd != null)
		{
			Point2D extra = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE, EXACTSELECTDISTANCE);
			directHitDist = extra.getX();
		}

		// look for text if a window was given
		if (findText && wnd != null)
		{
			// start by examining all text on this Cell
			Poly [] polys = cell.getAllText(findSpecial, wnd);
			if (polys != null)
			{
				for(int i=0; i<polys.length; i++)
				{
					Poly poly = polys[i];
					poly.setExactTextBounds(wnd);
					double dist = poly.polyDistance(bounds);
					if (dist >= directHitDist) continue;
					Highlight h = new Highlight(Type.TEXT);
					h.setCell(cell);
					h.setVar(poly.getVariable());
					list.add(h);
				}
			}

			// next examine all text on nodes in the cell
			for(Iterator it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				AffineTransform trans = ni.rotateOut();
				polys = ni.getAllText(findSpecial, wnd);
				if (polys == null) continue;
				for(int i=0; i<polys.length; i++)
				{
					Poly poly = polys[i];

					// do we need to do this? !!!
					poly.transform(trans);
					poly.setExactTextBounds(wnd);
					double dist = poly.polyDistance(bounds);
					if (dist >= directHitDist) continue;
					Highlight h = new Highlight(Type.TEXT);
					h.setCell(cell);
					h.setVar(poly.getVariable());
					h.setName(poly.getName());
					h.setPort(poly.getPort());
					h.setGeom(ni);
					list.add(h);
				}
			}

			// next examine all text on arcs in the cell
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				polys = ai.getAllText(findSpecial, wnd);
				if (polys == null) continue;
				for(int i=0; i<polys.length; i++)
				{
					Poly poly = polys[i];
					poly.setExactTextBounds(wnd);
					double dist = poly.polyDistance(bounds);
					if (dist >= directHitDist) continue;
					Highlight h = new Highlight(Type.TEXT);
					h.setCell(cell);
					h.setVar(poly.getVariable());
					h.setName(poly.getName());
					h.setPort(poly.getPort());
					h.setGeom(ai);
					list.add(h);
				}
			}
		}

		// determine proper area to search
		Rectangle2D searchArea = new Rectangle2D.Double(bounds.getMinX() - directHitDist,
			bounds.getMinY() - directHitDist, bounds.getWidth()+directHitDist*2, bounds.getHeight()+directHitDist*2);

		// now do 3 phases of examination: cells, arcs, then primitive nodes
		for(int phase=0; phase<3; phase++)
		{
			// ignore cells if requested
//			if (phase == 0 && !findSpecial && (us_useroptions&NOINSTANCESELECT) != 0) continue;

			// examine everything in the area
			Geometric.Search sea = new Geometric.Search(searchArea, cell);
			for(;;)
			{
				Geometric geom = sea.nextObject();
				if (geom == null) break;

				Highlight h;
				switch (phase)
				{
					case 0:			// check Cell instances
						if (geom instanceof ArcInst) break;
						if (((NodeInst)geom).getProto() instanceof PrimitiveNode) break;
						h = checkOutObject(geom, findPort, findSpecial, bounds, wnd, directHitDist);
						if (h != null) list.add(h);
						break;
					case 1:			// check arcs
						if (geom instanceof NodeInst) break;
						h = checkOutObject(geom, findPort, findSpecial, bounds, wnd, directHitDist);
						if (h != null) list.add(h);
						break;
					case 2:			// check primitive nodes
						if (geom instanceof ArcInst) break;
						if (((NodeInst)geom).getProto() instanceof Cell) break;
						h = checkOutObject(geom, findPort, findSpecial, bounds, wnd, directHitDist);
						if (h != null) list.add(h);
						break;
				}
			}
		}
		return list;
	}

	/**
	 * Routine to determine whether an object is in a bounds.
	 * @param geom the Geometric being tested for selection.
	 * @param findPort true if a port should be selected with a NodeInst.
	 * @param findSpecial true if hard-to-select and other special selection is being done.
	 * @param bounds the selected area or point.
	 * @param wnd the window being examined (null to ignore window scaling).
	 * @param directHitDist the slop area to forgive when searching (a few pixels in screen space, transformed to database units).
	 * @return a Highlight that defines the object, or null if the point is not over any part of this object.
	 */
	private static Highlight checkOutObject(Geometric geom, boolean findPort, boolean findSpecial, Rectangle2D bounds,
		EditWindow wnd, double directHitDist)
	{
		if (geom instanceof NodeInst)
		{
			// examine a node object
			NodeInst ni = (NodeInst)geom;

			// do not "find" hard-to-find nodes if "findSpecial" is not set
			if (!findSpecial && ni.isHardSelect()) return null;

			// do not include primitives that have all layers invisible
//			if (ni.getProto() instanceof PrimitiveNode && (ni->proto->userbits&NINVISIBLE) != 0) return;

			// do not "find" Invisible-Pins if they have text or exports
			if (ni.isInvisiblePinWithText()) return null;

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
					double bestDist = Double.MAX_VALUE;
					PortInst bestPort = null;
					for(Iterator it = ni.getPortInsts(); it.hasNext(); )
					{
						PortInst pi = (PortInst)it.next();
						Poly poly = pi.getPoly();
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
	 * @param wnd the window being examined (null to ignore text/window scaling).
	 * @return the distance from the bounds to the NodeInst.
	 * Negative values are direct hits.
	 */
	private static double distToNode(Rectangle2D bounds, NodeInst ni, EditWindow wnd)
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
		Rectangle2D niBounds = ni.getBounds();
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
	 * @param wnd the window being examined.
	 * @return the distance from the bounds to the ArcInst.
	 * Negative values are direct hits or intersections.
	 */
	private static double distToArc(Rectangle2D bounds, ArcInst ai, EditWindow wnd)
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

	/**
	 * Routine to draw an array of points as highlighting.
	 * @param wnd the window in which drawing is happening.
	 * @param g the Graphics for the window.
	 * @param points the array of points being drawn.
	 * @param offX the X offset of the drawing.
	 * @param offY the Y offset of the drawing.
	 * @param opened true if the points are drawn "opened".
	 * False to close the polygon.
	 */
	private static void drawOutlineFromPoints(EditWindow wnd, Graphics g, Point2D [] points, int offX, int offY, boolean opened)
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
		for(int i=1; i<points.length; i++)
		{
			int lastI = i-1;
			Point lp = wnd.databaseToScreen(points[lastI].getX(), points[lastI].getY());
			Point p = wnd.databaseToScreen(points[i].getX(), points[i].getY());
			g.drawLine(lp.x + offX, lp.y + offY, p.x + offX, p.y + offY);
		}
		if (!opened)
		{
			Point lp = wnd.databaseToScreen(points[0].getX(), points[0].getY());
			int lastI = points.length-1;
			Point p = wnd.databaseToScreen(points[lastI].getX(), points[lastI].getY());
			g.drawLine(lp.x + offX, lp.y + offY, p.x + offX, p.y + offY);
		}
	}

	/**
	 * Routine to tell whether two Highlights are the same.
	 * @param other the Highlight to compare to this one.
	 * @return true if the two refer to the same thing.
	 */
	private boolean sameThing(Highlight other)
	{
		if (type != other.getType()) return false;
		if (type == Type.BBOX || type == type.LINE) return false;
		if (geom != other.getGeom()) return false;
		if (type == Type.TEXT)
		{
			if (var != other.getVar()) return false;
			if (name != other.getName()) return false;
			if (cell != other.getCell()) return false;
			if (geom != other.getGeom()) return false;
			if (port != other.getPort()) return false;
		}
		return true;
	}

}
