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
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.UserMenuCommands;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ui.UIEdit;

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
 */

public class Highlight
{
	/** The highlighted object. */									Geometric geom;
	/** The highlighted port (if a NodeInst is highlighted). */		PortProto port;

	/** Screen offset for display of highlighting. */				private static int highOffX, highOffY;
	/** the highlighted objects. */									private static List highlightList = new ArrayList();

	private Highlight() {}

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
	public static Highlight addHighlighting(Geometric geom)
	{
		Highlight h = new Highlight();
		h.geom = geom;
		h.port = null;

		highlightList.add(h);
		return h;
	}

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
	 * Routine to return the Geometric associated with this Highlight object.
	 * @return the Geometric associated with this Highlight object.
	 */
	public Geometric getGeom() { return geom; }

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
	
	public static void selectArea(UIEdit wnd, double minSelX, double maxSelX, double minSelY, double maxSelY)
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
			Highlight h = addHighlighting(nextGeom);
		}
	}

	public static boolean overHighlighted(UIEdit wnd, int oldx, int oldy)
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
			for(Iterator it = getHighlights(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				Geometric highGeom = h.getGeom();
				if (highGeom == nextGeom) return true;
			}
		}
		return false;	
	}

	/**
	 * Routine to display this Highlight in a window.
	 * @param wnd the window in which to draw the highlight.
	 * @param g the Graphics associated with the window.
	 */
	public void showHighlight(UIEdit wnd, Graphics g)
	{
		g.setColor(Color.white);
		if (geom instanceof ArcInst)
		{
			// get information about the arc
			ArcInst ai = (ArcInst)geom;
			double offset = ai.getProto().getWidthOffset();

			// construct the polygons that describe the basic arc
			Poly poly = ai.makePoly(ai.getXSize(), ai.getWidth() - offset, Poly.Type.CLOSED);
			if (poly == null) return;
			Point2D.Double [] points = poly.getPoints();
			drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
			return;
		}

		// highlight a NodeInst
		NodeInst ni = (NodeInst)geom;
		NodeProto np = ni.getProto();
		SizeOffset so = new SizeOffset(0, 0, 0, 0);
		if (np instanceof PrimitiveNode)
		{
			PrimitiveNode pnp = (PrimitiveNode)np;
			so = Technology.getSizeOffset(ni);

			// special case for outline nodes
			int [] specialValues = pnp.getSpecialValues();
			if (np.isHoldsOutline())
			{
				Float [] outline = ni.getTrace();
				if (outline != null)
				{
					int numPoints = outline.length / 2;
					Point2D.Double [] pointList = new Point2D.Double[numPoints];
					for(int i=0; i<numPoints; i++)
					{
						pointList[i] = new Point2D.Double(ni.getCenterX() + outline[i*2].floatValue(),
							ni.getCenterY() + outline[i*2+1].floatValue());
					}
					drawOutlineFromPoints(wnd, g,  pointList, highOffX, highOffY);
				}
			}
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
			AffineTransform trans = ni.rotateOut();
			poly.transform(trans);
			Point2D.Double [] points = poly.getPoints();
			drawOutlineFromPoints(wnd, g,  points, highOffX, highOffY);
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
	void findObject(Point2D.Double pt, UIEdit win, Highlight curhigh,
		boolean exclusively, boolean another, boolean findport, boolean under, boolean special)
	{
		// initialize
		double bestdist = Double.MAX_VALUE;
		boolean looping = false;
		
		Highlight best, bestDirect, lastDirect, prevDirect;
//		best.fromgeom = NOGEOM;             best.status = 0;
//		bestdirect.fromgeom = NOGEOM;       bestdirect.status = 0;
//		lastdirect.fromgeom = NOGEOM;       lastdirect.status = 0;
//		prevdirect.fromgeom = NOGEOM;       prevdirect.status = 0;

		// ignore cells if requested
		int startphase = 0;
//		if (!special && (us_useroptions&NOINSTANCESELECT) != 0) startphase = 1;

		// search the relevant objects in the circuit
		Cell np = win.getCell();
		for(int phase = startphase; phase < 3; phase++)
		{
	//		recursivelySearch(np, exclusively, another, findport,
	//			under, special, curhigh, best, bestdirect, lastdirect, prevdirect, &looping,
	//				bestdist, pt, win, phase);
//			us_fartextsearch(np, exclusively, another, findport,
//				under, special, curhigh, &best, &bestdirect, &lastdirect, &prevdirect, &looping,
//					&bestdist, wantx, wanty, win, phase);
		}

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

		// use best direct hit if one exists, otherwise best any-kind-of-hit
//		if (bestDirect.status != 0)
//		{
//			curhigh->status = bestdirect.status;
//			curhigh->fromgeom = bestdirect.fromgeom;
//			curhigh->fromvar = bestdirect.fromvar;
//			curhigh->fromvarnoeval = bestdirect.fromvarnoeval;
//			curhigh->fromport = bestdirect.fromport;
//			curhigh->snapx = bestdirect.snapx;
//			curhigh->snapy = bestdirect.snapy;
//		} else
//		{
//			if (under == 0)
//			{
//				curhigh->status = best.status;
//				curhigh->fromgeom = best.fromgeom;
//				curhigh->fromvar = best.fromvar;
//				curhigh->fromvarnoeval = best.fromvarnoeval;
//				curhigh->fromport = best.fromport;
//				curhigh->snapx = best.snapx;
//				curhigh->snapy = best.snapy;
//			} else
//			{
//				curhigh->status = 0;
//				curhigh->fromgeom = NOGEOM;
//				curhigh->fromvar = NOVARIABLE;
//				curhigh->fromvarnoeval = NOVARIABLE;
//				curhigh->fromport = NOPORTPROTO;
//				curhigh->frompoint = 0;
//			}
//		}

		// see if looping through direct hits
//		if (looping != 0)
//		{
//			// made direct hit on previously selected object: looping through
//			if (prevdirect.status != 0)
//			{
//				curhigh->status = prevdirect.status;
//				curhigh->fromgeom = prevdirect.fromgeom;
//				curhigh->fromvar = prevdirect.fromvar;
//				curhigh->fromvarnoeval = prevdirect.fromvarnoeval;
//				curhigh->fromport = prevdirect.fromport;
//				curhigh->snapx = prevdirect.snapx;
//				curhigh->snapy = prevdirect.snapy;
//			} else if (lastdirect.status != 0)
//			{
//				curhigh->status = lastdirect.status;
//				curhigh->fromgeom = lastdirect.fromgeom;
//				curhigh->fromvar = lastdirect.fromvar;
//				curhigh->fromvarnoeval = lastdirect.fromvarnoeval;
//				curhigh->fromport = lastdirect.fromport;
//				curhigh->snapx = lastdirect.snapx;
//				curhigh->snapy = lastdirect.snapy;
//			}
//		}
//
//		if (curhigh->fromgeom == NOGEOM) curhigh->cell = np; else
//			curhigh->cell = geomparent(curhigh->fromgeom);
//
//		// quit now if nothing found
//		if (curhigh->status == 0) return;
//
//		// reevaluate if this is code
//		if ((curhigh->status&HIGHTYPE) == HIGHTEXT && curhigh->fromvar != NOVARIABLE &&
//			curhigh->fromvarnoeval != NOVARIABLE &&
//				curhigh->fromvar != curhigh->fromvarnoeval)
//					curhigh->fromvar = evalvar(curhigh->fromvarnoeval, 0, 0);
//
//		// find the closest port if this is a nodeinst and no port hit directly
//		if ((curhigh->status&HIGHTYPE) == HIGHFROM && curhigh->fromgeom->entryisnode &&
//			curhigh->fromport == NOPORTPROTO)
//		{
//			ni = curhigh->fromgeom->entryaddr.ni;
//			for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//			{
//				shapeportpoly(ni, pp, poly, FALSE);
//
//				// get distance of desired point to polygon
//				dist = polydistance(poly, wantx, wanty);
//				if (dist < 0)
//				{
//					curhigh->fromport = pp;
//					break;
//				}
//				if (curhigh->fromport == NOPORTPROTO) bestdist = dist;
//				if (dist > bestdist) continue;
//				bestdist = dist;   curhigh->fromport = pp;
//			}
//		}
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
	 * routine to search a Cell for objects that are close to (wantx, wanty).
	 * Those that are found are passed to "us_checkoutobject"
	 * for proximity evaluation, along with the evaluation parameters "curhigh",
	 * "best", "bestdirect", "lastdirect", "prevdirect", "looping", "bestdist",
	 * "exclusively", "another", "findport", and "under".  The "phase" value ranges
	 * from 0 to 2 according to the type of object desired.
	 */
//	void recursivelySearch(Cell cell, boolean exclusively, boolean another, boolean findport,
//		boolean under, boolean findspecial, Highlight curhigh, Highlight best, Highlight bestdirect,
//		Highlight lastdirect, Highlight prevdirect, INTBIG *looping, INTBIG *bestdist,
//		Point2D.Double pt, UIEdit wnd, int phase)
//	{
//		boolean found = false;
//		double bestdisttort = 1000.0;		// want biggest number possible
//		double slop = FARTEXTLIMIT;
//		Point2D.Double extra = wnd.deltaScreenToDatabase(EXACTSELECTDISTANCE, EXACTSELECTDISTANCE);
//		double directHitDist = extra.getX();
//		if (directHitDist > slop) slop = directHitDist;
//
//		Rectangle2D.Double searchArea = new Rectangle2D.Double(wantx - slop, wantx + slop,
//			wanty - slop, wanty + slop);
//		Geometric.Search sea = new Geometric.Search(searchArea, wnd.getCell());
//		for(;;)
//		{
//			Geometric geom = sea.nextObject();
//			if (geom == null) break;
//
//			// accumulate best R-tree module in case none are direct hits
//			Rectangle2D.Double bounds = geom.getBounds();
//			double disttort = Math.abs(pt.getX() - bounds.getCenterX()) + Math.abs(pt.getY() - bounds.getCenterY());
//			if (disttort < bestdisttort)
//			{
//				bestdisttort = disttort;
//				bestrt = i;
//			}
//
//			// see if this R-tree node is a direct hit
//			if (!exclusively && !bounds.intersects(searchArea)) continue;
//			found = true;
//
//			switch (phase)
//			{
//				case 0:			// only allow complex nodes
//					if (geom instanceof ArcInst) break;
//					if (((NodeInst)geom).getProto() instanceof PrimitiveNode) break;
//					us_checkoutobject(geom, false, exclusively, another, findport, findspecial,
//						curhigh, best, bestdirect, lastdirect, prevdirect, looping,
//							bestdist, pt, win);
//					break;
//				case 1:			// only allow arcs
//					if (geom instanceof NodeInst) break;
//					us_checkoutobject(geom, false, exclusively, another, findport, findspecial,
//						curhigh, best, bestdirect, lastdirect, prevdirect, looping,
//							bestdist, pt, win);
//					break;
//				case 2:			// only allow primitive nodes
//					if (geom instanceof NodeInst) break;
//					if (((NodeInst)geom).getProto() instanceof Cell) break;
//					us_checkoutobject(geom, false, exclusively, another, findport, findspecial,
//						curhigh, best, bestdirect, lastdirect, prevdirect, looping,
//							bestdist, pt, win);
//					break;
//			}
//		}
//	}

	/**
	 * search helper routine to include object "geom" in the search for the
	 * closest object to the cursor position at (wantx, wanty) in window "win".
	 * If "fartext" is nonzero, only look for far-away text on the object.
	 * If "exclusively" is nonzero, ignore nodes or arcs that are not of the
	 * current type.  If "another" is nonzero, ignore text objects.  If "findport"
	 * is nonzero, ports are being selected so cell names should not.  The closest
	 * object is "*bestdist" away and is described in "best".  The closest direct
	 * hit is in "bestdirect".  If that direct hit is the same as the last hit
	 * (kept in "curhigh") then the last direct hit (kept in "lastdirect") is
	 * moved to the previous direct hit (kept in "prevdirect") and the "looping"
	 * flag is set.  This indicates that the "prevdirect" object should be used
	 * (if it exists) and that the "lastdirect" object should be used failing that.
	 */
//	void us_checkoutobject(Geometric geom, boolean fartext, boolean exclusively, boolean another,
//		boolean findport, boolean findspecial, Highlight curhigh, Highlight best,
//		Highlight bestdirect, Highlight lastdirect, Highlight prevdirect, INTBIG *looping,
//		INTBIG *bestdist, Point2D.Double pt, UIEdit win)
//	{
//		// compute threshold for direct hits
//		double directHitDist = muldiv(EXACTSELECTDISTANCE, win->screenhx - win->screenlx, win->usehx - win->uselx);
//
//		if (geom instanceof NodeInst)
//		{
//			// examine a node object
//			NodeInst ni = (NodeInst)geom;
//
//			// do not "find" hard-to-find nodes if "findspecial" is not set
//			if (!findspecial && (ni->userbits&HARDSELECTN) != 0) return;

			// do not include primitives that have all layers invisible
//			if (ni.getProto() instanceof PrimitiveNode && (ni->proto->userbits&NINVISIBLE) != 0) return;

			// skip if being exclusive
//			if (exclusively && ni->proto != us_curnodeproto) return;

			// try text on the node (if not searching for "another")
//			if (!another && !exclusively)
//			{
//				us_initnodetext(ni, findspecial, win);
//				for(;;)
//				{
//					if (us_getnodetext(ni, win, poly, &var, &varnoeval, &port)) break;
//
//					// get distance of desired point to polygon
//					dist = polydistance(poly, wantx, wanty);
//
//					// direct hit
//					if (dist < directHitDist)
//					{
//						if (curhigh->fromgeom == geom && (curhigh->status&HIGHTYPE) == HIGHTEXT &&
//							curhigh->fromvar == var && curhigh->fromport == port)
//						{
//							*looping = 1;
//							prevdirect->status = lastdirect->status;
//							prevdirect->fromgeom = lastdirect->fromgeom;
//							prevdirect->fromvar = lastdirect->fromvar;
//							prevdirect->fromvarnoeval = lastdirect->fromvarnoeval;
//							prevdirect->fromport = lastdirect->fromport;
//						}
//						lastdirect->status = HIGHTEXT;
//						lastdirect->fromgeom = geom;
//						lastdirect->fromport = port;
//						lastdirect->fromvar = var;
//						lastdirect->fromvarnoeval = varnoeval;
//						if (dist < *bestdist)
//						{
//							bestdirect->status = HIGHTEXT;
//							bestdirect->fromgeom = geom;
//							bestdirect->fromvar = var;
//							bestdirect->fromvarnoeval = varnoeval;
//							bestdirect->fromport = port;
//						}
//					}
//
//					// see if it is closer than others
//					if (dist < *bestdist)
//					{
//						best->status = HIGHTEXT;
//						best->fromgeom = geom;
//						best->fromvar = var;
//						best->fromvarnoeval = varnoeval;
//						best->fromport = port;
//						*bestdist = dist;
//					}
//				}
//			}
//
//			if (fartext != 0) return;
//
//			// do not "find" Invisible-Pins if they have text or exports
//			if (ni.getProto() == Generic.tech.invisiblePin_node)
//			{
//				if (ni.getNumExports() != 0) return;
//				if (ni.numDisplayableVariables() != 0) return;
//			}
//
//			// get the distance to the object
//			double dist = distToObject(pt, geom);
//
//			// direct hit
//			if (dist < directHitDist)
//			{
//				if (curhigh->fromgeom == geom && (curhigh->status&HIGHTYPE) != HIGHTEXT)
//				{
//					*looping = 1;
//					prevdirect->status = lastdirect->status;
//					prevdirect->fromgeom = lastdirect->fromgeom;
//					prevdirect->fromvar = lastdirect->fromvar;
//					prevdirect->fromvarnoeval = lastdirect->fromvarnoeval;
//					prevdirect->fromport = lastdirect->fromport;
//					prevdirect->snapx = lastdirect->snapx;
//					prevdirect->snapy = lastdirect->snapy;
//
//					// see if there is another port under the cursor
//					if (curhigh->fromport != NOPORTPROTO)
//					{
//						for(pp = curhigh->fromport->nextportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//						{
//							shapeportpoly(ni, pp, poly, FALSE);
//							if (isinside(wantx, wanty, poly))
//							{
//								prevdirect->status = HIGHFROM;
//								prevdirect->fromgeom = geom;
//								prevdirect->fromport = pp;
//								break;
//							}
//						}
//					}
//				}
//				lastdirect->status = HIGHFROM;
//				lastdirect->fromgeom = geom;
//				lastdirect->fromport = NOPORTPROTO;
//				us_selectsnap(lastdirect, wantx, wanty);
//				if (dist < *bestdist)
//				{
//					bestdirect->status = HIGHFROM;
//					bestdirect->fromgeom = geom;
//					bestdirect->fromport = NOPORTPROTO;
//					us_selectsnap(bestdirect, wantx, wanty);
//				}
//			}
//
//			// see if it is closer than others
//			if (dist < *bestdist)
//			{
//				best->status = HIGHFROM;
//				best->fromgeom = geom;
//				best->fromport = NOPORTPROTO;
//				us_selectsnap(best, wantx, wanty);
//				*bestdist = dist;
//			}
//		} else
//		{
//			// examine an arc object
//			ai = geom->entryaddr.ai;
//
//			// do not "find" hard-to-find arcs if "findspecial" is not set
//			if (findspecial == 0 && (ai->userbits&HARDSELECTA) != 0) return;
//
//			// do not include arcs that have all layers invisible
//			if ((ai->proto->userbits&AINVISIBLE) != 0) return;
//
//			// skip if being exclusive
//			if (exclusively != 0 && ai->proto != us_curarcproto) return;
//
//			// try text on the arc (if not searching for "another")
//			if (exclusively == 0)
//			{
//				us_initarctext(ai, findspecial, win);
//				for(;;)
//				{
//					if (us_getarctext(ai, win, poly, &var, &varnoeval)) break;
//
//					// get distance of desired point to polygon
//					dist = polydistance(poly, wantx, wanty);
//
//					// direct hit
//					if (dist < directHitDist)
//					{
//						if (curhigh->fromgeom == geom && (curhigh->status&HIGHTYPE) == HIGHTEXT &&
//							curhigh->fromvar == var)
//						{
//							*looping = 1;
//							prevdirect->status = lastdirect->status;
//							prevdirect->fromgeom = lastdirect->fromgeom;
//							prevdirect->fromvar = lastdirect->fromvar;
//							prevdirect->fromvarnoeval = lastdirect->fromvarnoeval;
//							prevdirect->fromport = lastdirect->fromport;
//						}
//						lastdirect->status = HIGHTEXT;
//						lastdirect->fromgeom = geom;
//						lastdirect->fromvar = var;
//						lastdirect->fromvarnoeval = varnoeval;
//						lastdirect->fromport = NOPORTPROTO;
//						if (dist < *bestdist)
//						{
//							bestdirect->status = HIGHTEXT;
//							bestdirect->fromgeom = geom;
//							bestdirect->fromvar = var;
//							bestdirect->fromvarnoeval = varnoeval;
//							us_selectsnap(bestdirect, wantx, wanty);
//							bestdirect->fromport = NOPORTPROTO;
//						}
//					}
//
//					// see if it is closer than others
//					if (dist < *bestdist)
//					{
//						best->status = HIGHTEXT;
//						best->fromgeom = geom;
//						best->fromvar = var;
//						best->fromvarnoeval = varnoeval;
//						best->fromport = NOPORTPROTO;
//						us_selectsnap(best, wantx, wanty);
//						*bestdist = dist;
//					}
//				}
//			}
//
//			if (fartext != 0) return;
//
//			// get distance to arc
//			dist = distToObject(wantx, wanty, geom);
//
//			// direct hit
//			if (dist < directHitDist)
//			{
//				if (curhigh->fromgeom == geom && (curhigh->status&HIGHTYPE) != HIGHTEXT)
//				{
//					*looping = 1;
//					prevdirect->status = lastdirect->status;
//					prevdirect->fromgeom = lastdirect->fromgeom;
//					prevdirect->fromvar = lastdirect->fromvar;
//					prevdirect->fromvarnoeval = lastdirect->fromvarnoeval;
//					prevdirect->fromport = lastdirect->fromport;
//					prevdirect->snapx = lastdirect->snapx;
//					prevdirect->snapy = lastdirect->snapy;
//				}
//				lastdirect->status = HIGHFROM;
//				lastdirect->fromgeom = geom;
//				lastdirect->fromport = NOPORTPROTO;
//				us_selectsnap(lastdirect, wantx, wanty);
//				if (dist < *bestdist)
//				{
//					bestdirect->status = HIGHFROM;
//					bestdirect->fromgeom = geom;
//					bestdirect->fromvar = NOVARIABLE;
//					bestdirect->fromvarnoeval = NOVARIABLE;
//					bestdirect->fromport = NOPORTPROTO;
//					us_selectsnap(bestdirect, wantx, wanty);
//				}
//			}
//
//			// see if it is closer than others
//			if (dist < *bestdist)
//			{
//				best->status = HIGHFROM;
//				best->fromgeom = geom;
//				best->fromvar = NOVARIABLE;
//				best->fromvarnoeval = NOVARIABLE;
//				best->fromport = NOPORTPROTO;
//				us_selectsnap(best, wantx, wanty);
//				*bestdist = dist;
//			}
//		}
//	}

	/**
	 * Routine to return the distance from a point to a Geometric.
	 * @param pt the point in question.
	 * @param geom the Geometric.
	 * @return the distance from the point to the Geometric.
	 * Negative values are direct hits.
	 */
	double distToObject(Point2D.Double pt, Geometric geom)
	{
		if (geom instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)geom;
			AffineTransform trans = ni.rotateOut();

			// special case for MOS transistors: examine the gate/active tabs
			NodeProto.Function fun = ni.getProto().getFunction();
			if (fun == NodeProto.Function.TRANMOS || fun == NodeProto.Function.TRAPMOS || fun == NodeProto.Function.TRADMOS)
			{
				Technology tech = ni.getProto().getTechnology();
				Poly [] polys = tech.getShape(ni);
				double bestDist = Double.MAX_VALUE;
				for(int box=0; box<polys.length; box++)
				{
					Poly poly = polys[box];
					Layer.Function lf = poly.getLayer().getFunction();
					if (lf.isPoly() && !lf.isDiff()) continue;
					poly.transform(trans);
					double dist = poly.polyDistance(pt);
					if (dist < bestDist) bestDist = dist;
				}
				return bestDist;
			}

			// special case for 1-polygon primitives: check precise distance to cursor
//			if (ni->proto->primindex != 0 && (ni->proto->userbits&NEDGESELECT) != 0)
//			{
//				count = nodepolys(ni, 0, NOWINDOWPART);
//				bestdist = MAXINTBIG;
//				for(box=0; box<count; box++)
//				{
//					shapenodepoly(ni, box, poly);
//					if ((poly->desc->colstyle&INVISIBLE) != 0) continue;
//					xformpoly(poly, trans);
//					dist = polydistance(poly, x, y);
//					if (dist < bestdist) bestdist = dist;
//				}
//				return(bestdist);
//			}

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
			return nodePoly.polyDistance(pt);
		}

		// determine distance to arc
		ArcInst ai = (ArcInst)geom;

		// if arc is selectable precisely, check distance to cursor
//		if ((ai->proto->userbits&AEDGESELECT) != 0)
//		{
//			count = arcpolys(ai, NOWINDOWPART);
//			bestdist = MAXINTBIG;
//			for(box=0; box<count; box++)
//			{
//				shapearcpoly(ai, box, poly);
//				if ((poly->desc->colstyle&INVISIBLE) != 0) continue;
//				dist = polydistance(poly, x, y);
//				if (dist < bestdist) bestdist = dist;
//			}
//			return(bestdist);
//		}

		// standard distance to the arc
		double wid = ai.getWidth() - ai.getProto().getWidthOffset();
		if (EMath.doublesEqual(wid, 0)) wid = 1;
//		if (curvedarcoutline(ai, poly, FILLED, wid))
			Poly poly = ai.makePoly(ai.getXSize(), wid, Poly.Type.FILLED);
		return poly.polyDistance(pt);
	}

	// ************************************* SUPPORT *************************************

	private static void drawOutlineFromPoints(UIEdit wnd, Graphics g, Point2D.Double [] points, int offX, int offY)
	{
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

