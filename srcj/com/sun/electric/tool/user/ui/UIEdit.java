/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UIEdit.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.VarContext;
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
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.UserMenuCommands;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
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
 * Created on Sep 25, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

public class UIEdit extends JPanel
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
{
	/** the window scale */									private double scale;
	/** the window offset */								private double offx, offy;
	/** the size of the window (in pixels) */				private Dimension sz;
	/** the cell that is in the window */					private Cell cell;
	/** Cell's VarContext */                                private VarContext cellVarContext;
    /** the offscreen image of the window */				private Image img = null;
	/** true if the window needs to be rerendered */		private boolean needsUpdate = false;
	/** true to track the time for redraw */				private boolean trackTime = false;
	/** the highlighted objects in this window */			private static List highlightList = new ArrayList();
	/** starting screen point for drags in this window */	private Point startDrag = new Point();
	/** ending screen point for drags in this window */		private Point endDrag = new Point();
	/** true if doing drags in this window */				private boolean doingDrag = false;
	/** true if showing grid in this window */				private boolean showGrid = false;
    

	/** an identity transformation */						private static final AffineTransform IDENTITY = new AffineTransform();
	/** the offset of each new window on the screen */		private static int windowOffset = 0;

	/** for drawing solid lines */		private static final BasicStroke solidLine = new BasicStroke(0);
	/** for drawing thick lines */		private static final BasicStroke thickLine = new BasicStroke(1);
	/** for drawing dotted lines */		private static final BasicStroke dottedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1}, 0);
	/** for drawing dashed lines */		private static final BasicStroke dashedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {10}, 0);
	/** for drawing selection boxes */	private static final BasicStroke selectionLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {2}, 3);


	// ************************************* CONTROL *************************************

	// constructor
	private UIEdit(Cell cell)
	{
		//super(cell.describe(), true, true, true, true);
		this.cell = cell;
        this.cellVarContext = VarContext.globalContext;
        
		sz = new Dimension(500, 500);
		setSize(sz.width, sz.height);
		setPreferredSize(sz);
		//setAutoscrolls(true);
		addKeyListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		addMouseListener(this);
	}

	// factory
	public static UIEdit CreateElectricDoc(Cell cell)
	{
		UIEdit ui = new UIEdit(cell);
		return ui;
	}

	public void redraw()
	{
		needsUpdate = true;
		repaint();
	}

	public void paint(Graphics g)
	{
		// to enable keys to be recieved
		requestFocus();
		if (img == null || !getSize().equals(sz))
		{
			if (cell == null) return;
			sz = getSize();
			img = createImage(sz.width, sz.height);
			fillScreen();
		}
		if (needsUpdate)
		{
			needsUpdate = false;
			drawImage();
		}
		g.drawImage(img, 0, 0, this);

		// add in grid
		if (showGrid) drawGrid(g);

		// add in highlighting
		for(Iterator it = highlightList.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom.getParent() != cell) continue;
			showHighlight(g, geom);
		}

		// add in drag area
		if (doingDrag)
			showDragBox(g);
	}

	public void setTimeTracking(boolean trackTime)
	{
		this.trackTime = trackTime;
	}

	/**
	 * Routine to set the cell that is shown in the window to "cell".
	 */
	public void setCell(Cell cell, VarContext context)
	{
		this.cell = cell;
        this.cellVarContext = context;
		Library curLib = Library.getCurrent();
		curLib.setCurCell(cell);
		clearHighlighting();
		fillScreen();
		redraw();
	}

	public void fillScreen()
	{
		if (cell == null) return;
		sz = getSize();
		Rectangle2D cellBounds = cell.getBounds();
		double scalex = sz.width/cellBounds.getWidth() * 0.9;
		double scaley = sz.height/cellBounds.getHeight() * 0.9;
		scale = Math.min(scalex, scaley);
		offx = cellBounds.getCenterX();
		offy = cellBounds.getCenterY();
		needsUpdate = true;
	}

    public void downHierarchy() {
        if (getNumHighlights() <= 0) {
            System.out.println("Nothing highlighted, cannot descend");
            return;
        }
        if (getNumHighlights() > 1) {
            System.out.println("More than one thing highlighted, cannot descend");
            return;
        }
        Iterator it = getHighlights();
        Geometric geom = (Geometric)it.next();
        if (!(geom instanceof NodeInst)) {
            System.out.println("Cannot descend into that object");
            return;
        }
        NodeInst ni = (NodeInst)geom;
        NodeProto np = ni.getProto();
        if (!(np instanceof Cell)) {
            //System.out.println("Cannot descend into that object");
            return;
        }
        Cell cell = (Cell)np;
        Cell schCell = cell.getEquivalent();
        // special case: if cell is icon of current cell, descend into icon
        if (this.cell == schCell) schCell = cell;
        if (schCell == null) return;                // nothing to descend into
        setCell(schCell, cellVarContext.push(ni));
        drawImage();
    }
    
    public void upHierarchy() {
        try {
            NodeInst ni = cellVarContext.getNodeInst();
            Cell parent = ni.getParent();
            VarContext context = cellVarContext.pop();
            setCell(parent, context);
            drawImage();
        } catch (NullPointerException e) {
            // no parent - if icon, go to sch view
            // - otherwise, ask user where to go if necessary
            if (cell.getView() == View.ICON) {
                Cell schCell = cell.getEquivalent();
                if (schCell == null) return;        // nothing to do
                setCell(schCell, VarContext.globalContext);
                drawImage();
                return;
            }
            // TODO: find possible parents, ask user to choose if needed
        }
    }

    // ************************************* RENDERING A WINDOW *************************************

	/**
	 * Routine to draw the current window.
	 */
	void drawImage()
	{
		// set background color
		Graphics2D g2 = (Graphics2D)img.getGraphics();
		g2.setColor(Color.lightGray);
		g2.fillRect(0, 0, sz.width, sz.height);
		if (cell == null) return;

		// setup graphics for rendering (start at bottom and work up)
		g2.translate(sz.width/2, sz.height/2);
		g2.scale(scale, -scale);
		g2.translate(-offx, -offy);

		// if tracking time, start the clock
		long startTime = 0;
		if (trackTime) startTime = System.currentTimeMillis();

		// draw everything
		drawCell(g2, cell, IDENTITY, true);

		// if tracking time, report the time
		if (trackTime)
		{
			long endTime = System.currentTimeMillis();
			float finalTime = (endTime - startTime) / 1000F;
			System.out.println("Took " + finalTime + " seconds to redisplay");
			trackTime = false;
		}
	}

	/**
	 * Routine to draw the contents of cell "c", transformed through "prevTrans".
	 */
	void drawCell(Graphics2D g2, Cell cell, AffineTransform prevTrans, boolean topLevel)
	{
		// draw all arcs
		Iterator arcs = cell.getArcs();
		while (arcs.hasNext())
		{
			drawArc(g2, (ArcInst)arcs.next(), prevTrans, topLevel);
		}

		// draw all nodes
		Iterator nodes = cell.getNodes();
		while (nodes.hasNext())
		{
			drawNode(g2, (NodeInst)nodes.next(), prevTrans, topLevel);
		}
	}

	/**
	 * Routine to draw node "ni", transformed through "trans".
	 */
	void drawNode(Graphics2D g2, NodeInst ni, AffineTransform trans, boolean topLevel)
	{
		NodeProto np = ni.getProto();
		
		// see if the node is completely clipped from the screen
//		Rectangle2D clipBound = ni.getBounds();
//		Poly clipPoly = new Poly(clipBound.getCenterX(), clipBound.getCenterY(), clipBound.getWidth(), clipBound.getHeight());
//		AffineTransform screen = g2.getTransform();
//		clipPoly.transform(screen);
//		clipBound = clipPoly.getBounds();
//System.out.println("node is "+clipBound.getWidth()+"x"+clipBound.getHeight()+" at ("+clipBound.getCenterX()+","+clipBound.getCenterY());

		AffineTransform localTrans = ni.rotateOut(trans);
		if (np instanceof Cell)
		{
			// cell instance
			Cell subCell = (Cell)np;

			// two ways to draw a cell instance
			if (ni.isExpanded())
			{
				// show the contents
				AffineTransform subTrans = ni.translateOut(localTrans);
				drawCell(g2, subCell, subTrans, false);
			} else
			{
				// draw the outline
				Rectangle2D cellBounds = ni.getBounds();
				Poly poly = new Poly(cellBounds.getCenterX(), cellBounds.getCenterY(), cellBounds.getWidth(), cellBounds.getHeight());
				g2.setColor(Color.black);
				g2.setStroke(solidLine);
				g2.draw(poly);
				Rectangle2D bounds = poly.getBounds2D();
				drawTextBoxed(g2, bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY(), np.describe(), 1.0, Color.black);

				// show the ports
				int numPorts = ni.getNumPortInsts();
				PortInst [] portlist = new PortInst[numPorts];
				int i=0;
				for(Iterator it = ni.getPortInsts(); it.hasNext();)
				{
					portlist[i++] = (PortInst) it.next();
				}
				for(Iterator it = ni.getConnections(); it.hasNext();)
				{
					Connection con = (Connection) it.next();
					PortInst pi = con.getPortInst();
					portlist[pi.getIndex()] = null;
				}
				for(Iterator it = ni.getExports(); it.hasNext();)
				{
					Export exp = (Export) it.next();
					PortInst pi = exp.getOriginalPort();
					portlist[pi.getIndex()] = null;
				}
				for(i=0; i<numPorts; i++)
				{
					if (portlist[i] == null) continue;
					Poly portPoly = portlist[i].getPoly();
					if (portPoly == null) continue;
					portPoly.transform(trans);
					drawTextCentered(g2, portPoly.getCenterX(), portPoly.getCenterY(), portlist[i].getPortProto().getProtoName(), 0.25, Color.red);
				}
				
				// show displayable variables on the instance
				int numPolys = ni.numDisplayableVariables();
				Poly [] polys = new Poly[numPolys];
				Rectangle2D rect = ni.getBounds();
				ni.addDisplayableVariables(rect, polys, 0);
				if (drawPolys(g2, polys, localTrans))
					System.out.println("... while displaying instance "+ni.describe() + " in cell " + ni.getParent().describe());
			}
		} else
		{
			// primitive
			if (topLevel || !ni.isVisInside())
			{
				PrimitiveNode prim = (PrimitiveNode)np;
				Technology tech = prim.getTechnology();
				Poly [] polys = tech.getShape(ni);
				if (drawPolys(g2, polys, localTrans))
					System.out.println("... while displaying node "+ni.describe() + " in cell " + ni.getParent().describe());
			}
		}

		// draw any exports from the node
		if (topLevel)
		{
			Iterator it = ni.getExports();
			while (it.hasNext())
			{
				Export e = (Export) it.next();
				Poly poly = e.getOriginalPort().getPoly();
				poly.transform(localTrans);
				drawTextCentered(g2, poly.getCenterX(), poly.getCenterY(), e.getProtoName(), 0.25, Color.black);
			}
		}
	}

	/**
	 * Routine to draw arc "ai", transformed through "trans".
	 */
	void drawArc(Graphics2D g2, ArcInst ai, AffineTransform trans, boolean topLevel)
	{
		ArcProto ap = ai.getProto();
		Technology tech = ap.getTechnology();
		Poly [] polys = tech.getShape(ai);
		if (drawPolys(g2, polys, trans))
			System.out.println("... while displaying arc "+ai.describe() + " in cell " + ai.getParent().describe());
	}

	/**
	 * Routine to draw polygon "poly", transformed through "trans".
	 * Returns true on error.
	 */
	boolean drawPolys(Graphics2D g2, Poly [] polys, AffineTransform trans)
	{
		if (polys == null) return false;
		for(int i = 0; i < polys.length; i++)
		{
			// get the polygon and transform it
			Poly poly = polys[i];
			if (poly == null)
			{
				System.out.println("Warning: poly " + i + " of list of " + polys.length + " is null");
				return true;
			}
			poly.transform(trans);

			// set the color
			Color color;
			Layer layer = poly.getLayer();
			if (layer == null) color = Color.black; else
			{
				EGraphics graphics = layer.getGraphics();
				color = graphics.getColor();
			}
			g2.setPaint(color);

			// now draw it
			Poly.Type style = poly.getStyle();
			if (style == Poly.Type.FILLED)
			{
				g2.fill(poly);
			} else if (style == Poly.Type.CLOSED || style == Poly.Type.OPENED || style == Poly.Type.OPENEDT1 ||
				style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3 || style == Poly.Type.OPENEDO1 ||
				style == Poly.Type.VECTORS)
			{
				drawOutline(g2, poly);
			} else if (style == Poly.Type.CROSSED)
			{
				g2.setStroke(solidLine);
				GeneralPath gp = new GeneralPath();
				Point2D [] points = poly.getPoints();
				gp.moveTo((float)points[0].getX(), (float)points[0].getY());
				for(int j=1; j<points.length; j++)
					gp.lineTo((float)points[j].getX(), (float)points[j].getY());
				gp.lineTo((float)points[0].getX(), (float)points[0].getY());
				gp.lineTo((float)points[2].getX(), (float)points[2].getY());
				gp.moveTo((float)points[1].getX(), (float)points[1].getY());
				gp.lineTo((float)points[3].getX(), (float)points[3].getY());
				g2.draw(gp);
			} else if (style == Poly.Type.TEXTCENT || style == Poly.Type.TEXTTOP || style == Poly.Type.TEXTBOT ||
				style == Poly.Type.TEXTLEFT || style == Poly.Type.TEXTRIGHT || style == Poly.Type.TEXTTOPLEFT ||
				style == Poly.Type.TEXTBOTLEFT || style == Poly.Type.TEXTTOPRIGHT || style == Poly.Type.TEXTBOTRIGHT ||
				style == Poly.Type.TEXTBOX)
			{
				double x = poly.getCenterX();
				double y = poly.getCenterY();
				TextDescriptor descript = poly.getTextDescriptor();
				drawTextCentered(g2, x, y, poly.getString(), 0.1, Color.black);
			} else if (style == Poly.Type.CIRCLE || style == Poly.Type.THICKCIRCLE || style == Poly.Type.DISC)
			{
				drawCircular(g2, poly);
			} else if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
			{
				AffineTransform saveAT = g2.getTransform();
				double scale = 100;
				g2.scale(1/scale, 1/scale);
				Point2D.Double [] points = poly.getPoints();
				int ctrX = (int)(points[0].getX() * scale);
				int ctrY = (int)(points[0].getY() * scale);
				int startX = (int)(points[1].getX() * scale);
				int startY = (int)(points[1].getY() * scale);
				int endX = (int)(points[2].getX() * scale);
				int endY = (int)(points[2].getY() * scale);
				int radius = (int)(points[0].distance(points[1]) * scale);
				int diameter = radius * 2;
				int startAngle = (int)(Math.atan2(startY-ctrY, startX-ctrX) * 180.0 / Math.PI);
				if (startAngle < 0) startAngle += 360;
				int endAngle = (int)(Math.atan2(endY-ctrY, endX-ctrX) * 180.0 / Math.PI);
				if (endAngle < 0) endAngle += 360;
				if (startAngle < endAngle) startAngle += 360;
				startAngle = 360 - startAngle;
				endAngle = 360 - endAngle;
				g2.drawArc(ctrX-radius, ctrY-radius, diameter, diameter, startAngle, endAngle - startAngle);
				g2.setTransform(saveAT);
			} else if (style == Poly.Type.CROSS || style == Poly.Type.BIGCROSS)
			{
				// draw the big cross
				drawCross(g2, poly);
			} else if (style == Poly.Type.GRIDDOTS)
			{
				System.out.println("Cannot render GRIDDOTS polygon");
			}
		}
		return false;
	}

	// ************************************* SPECIAL SHAPE DRAWING *************************************

	/**
	 * Routine to draw a large or small cross, as described in "poly".
	 */
	void drawCross(Graphics2D g2, Poly poly)
	{
		float x = (float)poly.getCenterX();
		float y = (float)poly.getCenterY();
		float size = 5 / (float)scale;
		if (poly.getStyle() == Poly.Type.CROSS) size = 3 / (float)scale;
		g2.setStroke(solidLine);
		g2.setColor(Color.black);
		GeneralPath gp = new GeneralPath();
		gp.moveTo(x+size, y);  gp.lineTo(x-size, y);
		gp.moveTo(x, y+size);  gp.lineTo(x, y-size);
		g2.draw(gp);
	}

	/**
	 * Routine to draw lines, as described in "poly".
	 */
	void drawOutline(Graphics2D g2, Poly poly)
	{
		Poly.Type theStyle = poly.getStyle();
		AffineTransform saveAT = g2.getTransform();
		double theScale = scale;
		if (theStyle == Poly.Type.OPENEDT3) theScale /= 2;
		g2.scale(1/theScale, 1/theScale);
		if (theStyle == Poly.Type.OPENEDT1) g2.setStroke(dottedLine); else
		if (theStyle == Poly.Type.OPENEDT2) g2.setStroke(dashedLine); else
		if (theStyle == Poly.Type.OPENEDT3) g2.setStroke(thickLine); else
			g2.setStroke(solidLine);

		GeneralPath gp = new GeneralPath();
		Point2D [] points = poly.getPoints();
		if (theStyle == Poly.Type.VECTORS)
		{
			for(int j=0; j<points.length; j+=2)
			{
				gp.moveTo((float)(points[j].getX()*theScale), (float)(points[j].getY()*theScale));
				gp.lineTo((float)(points[j+1].getX()*theScale), (float)(points[j+1].getY()*theScale));
			}
		} else
		{
			gp.moveTo((float)(points[0].getX()*theScale), (float)(points[0].getY()*theScale));
			for(int j=1; j<points.length; j++)
			{
				gp.lineTo((float)(points[j].getX()*theScale), (float)(points[j].getY()*theScale));
			}
			if (theStyle == Poly.Type.CLOSED)
				gp.lineTo((float)(points[0].getX()*theScale), (float)(points[0].getY()*theScale));
		}
		g2.draw(gp);
		g2.setTransform(saveAT);
	}

	/**
	 * Routine to draw a circle or a disc, as described in "poly".
	 */
	void drawCircular(Graphics2D g2, Poly poly)
	{
		AffineTransform saveAT = g2.getTransform();
		double cScale = 100;
//		if (poly.getStyle() == Poly.Type.THICKCIRCLE) g2.setStroke(thickLine); else
//			g2.setStroke(solidLine);
		g2.scale(1/cScale, 1/cScale);
		Point2D [] points = poly.getPoints();
		int ctrX = (int)(points[0].getX() * cScale);
		int ctrY = (int)(points[0].getY() * cScale);
		int edgeX = (int)(points[1].getX() * cScale);
		int edgeY = (int)(points[1].getY() * cScale);
		int radius;
		if (edgeX == ctrX) radius = Math.abs(ctrY - edgeY); else
			if (edgeY == ctrY) radius = Math.abs(ctrX - edgeX); else
				radius = (int)Math.sqrt((ctrY - edgeY)*(ctrY - edgeY) + (ctrX - edgeX) * (ctrX - edgeX));
		int diameter = radius * 2;
		if (poly.getStyle() == Poly.Type.DISC)
		{
			g2.fillOval(ctrX-radius, ctrY-radius, diameter, diameter);
		} else
		{
			g2.drawOval(ctrX-radius, ctrY-radius, diameter, diameter);
		}
		g2.setTransform(saveAT);
	}

	void drawTextCentered(Graphics2D g2, double x, double y, String text, double textScale, Color color)
	{
		// make a glyph vector for the desired text
		Font font = g2.getFont();
		FontRenderContext frc = new FontRenderContext(null, false, false);
		GlyphVector gv = font.createGlyphVector(frc, text);

		// adjust to place text in the center
		Rectangle2D glyphBounds = gv.getVisualBounds();
		x -= glyphBounds.getCenterX() * textScale;
		y += glyphBounds.getCenterY() * textScale;

		// draw the text
		AffineTransform saveAT = g2.getTransform();
		g2.translate(x, y);
		g2.scale(textScale, -textScale);
		g2.translate(-x, -y);
		g2.setColor(color);
		g2.drawGlyphVector(gv, (float)x, (float)y);
		g2.setTransform(saveAT);
	}

	void drawTextBoxed(Graphics2D g2, double lowX, double highX, double lowY, double highY, String text, double textScale, Color color)
	{
		// make a glyph vector for the desired text
		Font font = g2.getFont();
		FontRenderContext frc = new FontRenderContext(null, false, false);
		GlyphVector gv = font.createGlyphVector(frc, text);

		// adjust to place text in the center
		Rectangle2D glyphBounds = gv.getVisualBounds();
		double textWidth = glyphBounds.getWidth() * textScale;
		double textHeight = glyphBounds.getHeight() * textScale;
		if (textWidth > highX - lowX)
		{
			// text too big for box: scale it down
			textScale *= (highX - lowX) / textWidth;
		} else
		{
			// text smaller than box: scale it up (do we really want to do this?)
			textScale *= (highX - lowX) / textWidth;
		}
		double x = (lowX + highX) / 2;
		double y = (lowY + highY) / 2;
		x -= glyphBounds.getCenterX() * textScale;
		y += glyphBounds.getCenterY() * textScale;

		// draw the text
		AffineTransform saveAT = g2.getTransform();
		g2.translate(x, y);
		g2.scale(textScale, -textScale);
		g2.translate(-x, -y);
		g2.setColor(color);
		g2.drawGlyphVector(gv, (float)x, (float)y);
		g2.setTransform(saveAT);
	}

	// ************************************* GRID *************************************

	/**
	 * Routine to set the display of a grid in this window.
	 * @param showGrid true to show the grid.
	 */
	public void setGrid(boolean showGrid)
	{
		this.showGrid = showGrid;
		redraw();
	}

	/**
	 * Routine to return the state of grid display in this window.
	 * @return true if the grid is displayed in this window.
	 */
	public boolean getGrid() { return showGrid; }

	/**
	 * Routine to display the grid.
	 */
	private void drawGrid(Graphics g)
	{
		/* grid spacing */
		int x0 = 1; int y0 = 1;

		// bold dot spacing
		int xspacing = 10, yspacing = 10;

		/* object space extent */
		Point2D.Double low = screenToDatabase(0, 0);
		double x4 = low.getX();  double y4 = low.getY();
		Point2D.Double high = screenToDatabase(sz.width, sz.height);
		double x5 = high.getX();  double y5 = high.getY();

		/* initial grid location */
		int x1 = ((int)x4) / x0 * x0;
		int y1 = ((int)y4) / y0 * y0;

		int xnum = sz.width;
		double xden = x5 - x4;
		int ynum = sz.height;
		double yden = y5 - y4;
		int x10 = x0*xspacing;
		int y10 = y0*yspacing;
		int y1base = y1 - (y1 / y0 * y0);
		int x1base = x1 - (x1 / x0 * x0);

		/* adjust grid placement according to scale */
		boolean fatdots = false;
		if (x0 * xnum / xden < 5 || y0 * ynum / (-yden) < 5)
		{
			x1 = x1base - (x1base - x1) / x10 * x10;   x0 = x10;
			y1 = y1base - (y1base - y1) / y10 * y10;   y0 = y10;
		} else if (x0 * xnum / xden > 75 && y0 * ynum / (-yden) > 75)
		{
			fatdots = true;
		}

		/* draw the grid to the offscreen buffer */
		g.setColor(Color.black);
		for(int i = y1; i > y5; i -= y0)
		{
			int y = (int)((i-y4) * ynum / yden);
			if (y < 0 || y > sz.height) continue;
			int y10mod = (i-y1base) % y10;
			for(int j = x1; j < x5; j += x0)
			{
				int x = (int)((j-x4) * xnum / xden);
				boolean everyTen = ((j-x1base)%x10) == 0 && y10mod == 0;
				if (fatdots && everyTen)
				{
					g.fillRect(x-2,y, 5, 1);
					g.fillRect(x,y-2, 1, 5);
					g.fillRect(x-1,y-1, 3, 3);
					continue;
				}

				/* special case every 10 grid points in each direction */
				if (fatdots || everyTen)
				{
					g.fillRect(x-1,y, 3, 1);
					g.fillRect(x,y-1, 1, 3);
					continue;
				}

				// just a single dot
				g.fillRect(x,y, 1, 1);

			}
		}
	}

	// ************************************* HIGHLIGHTING *************************************

	/**
	 * Routine to clear the list of highlighted objects.
	 */
	public static void clearHighlighting()
	{
		highlightList.clear();
	}

	/**
	 * Routine to add a Geometric to the list of highlighted objects.
	 * @param geom the Geometric to add to the list of highlighted objects.
	 */
	public static void addHighlighting(Geometric geom)
	{
		Cell parent = geom.getParent();
		highlightList.add(geom);
	}

	/**
	 * Routine to return the number of highlighted objects.
	 * @return the number of highlighted objects.
	 */
	public static int getNumHighlights()
	{
		return highlightList.size();
	}

	/**
	 * Routine to return an Iterator over the highlighted objects.
	 * @return an Iterator over the highlighted objects.
	 */
	public static Iterator getHighlights()
	{
		return highlightList.iterator();
	}

	private void showHighlight(Graphics g, Geometric geom)
	{
		g.setColor(Color.white);
		Rectangle2D rect = geom.getBounds();
		if (rect.getWidth() == 0 && rect.getHeight() == 0)
		{
			float x = (float)rect.getCenterX();
			float y = (float)rect.getCenterY();
			GeneralPath gp = new GeneralPath();
			float size = 3 / (float)scale;
			Point c1 = databaseToScreen(x+size, y);
			Point c2 = databaseToScreen(x-size, y);
			Point c3 = databaseToScreen(x, y+size);
			Point c4 = databaseToScreen(x, y-size);
			g.drawLine(c1.x, c1.y, c2.x, c2.y);
			g.drawLine(c3.x, c3.y, c4.x, c4.y);
		} else
		{
			Point c1 = databaseToScreen(rect.getMinX(), rect.getMinY());
			Point c2 = databaseToScreen(rect.getMinX(), rect.getMaxY());
			Point c3 = databaseToScreen(rect.getMaxX(), rect.getMaxY());
			Point c4 = databaseToScreen(rect.getMaxX(), rect.getMinY());
			g.drawLine(c1.x, c1.y, c2.x, c2.y);
			g.drawLine(c2.x, c2.y, c3.x, c3.y);
			g.drawLine(c3.x, c3.y, c4.x, c4.y);
			g.drawLine(c4.x, c4.y, c1.x, c1.y);
		}
	}

	private void showDragBox(Graphics g)
	{
		int lX = (int)Math.min(startDrag.getX(), endDrag.getX());
		int hX = (int)Math.max(startDrag.getX(), endDrag.getX());
		int lY = (int)Math.min(startDrag.getY(), endDrag.getY());
		int hY = (int)Math.max(startDrag.getY(), endDrag.getY());
		Graphics2D g2 = (Graphics2D)g;
		g2.setStroke(selectionLine);
		g.setColor(Color.white);
		g.drawLine(lX, lY, lX, hY);
		g.drawLine(lX, hY, hX, hY);
		g.drawLine(hX, hY, hX, lY);
		g.drawLine(hX, lY, lX, lY);
	}
	// ************************************* WINDOW INTERACTION *************************************

	private int oldx, oldy;

	public Point2D.Double screenToDatabase(int screenX, int screenY)
	{
		double dbX = (screenX - sz.width/2) / scale + offx;
		double dbY = (sz.height/2 - screenY) / scale + offy;
		return new Point2D.Double(dbX, dbY);
	}

	public Point databaseToScreen(double dbX, double dbY)
	{
		int screenX = (int)(sz.width/2 + (dbX - offx) * scale);
		int screenY = (int)(sz.height/2 - (dbY - offy) * scale);
		return new Point(screenX, screenY);
	}

	public void mousePressed(MouseEvent evt)
	{
		oldx = evt.getX();
		oldy = evt.getY();

		if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) != 0) return;
		if ((evt.getModifiers()&MouseEvent.CTRL_MASK) != 0) return;

		// standard selection: drag out a selection box
		startDrag.setLocation(oldx, oldy);
		endDrag.setLocation(oldx, oldy);
		doingDrag = true;
		repaint();
	}

	private static final int EXACTSELECTDISTANCE = 5;
    
	public void mouseReleased(MouseEvent evt)
	{
		if (doingDrag)
		{
			clearHighlighting();
			double minSelX = Math.min(startDrag.getX(), endDrag.getX())-EXACTSELECTDISTANCE;
			double maxSelX = Math.max(startDrag.getX(), endDrag.getX())+EXACTSELECTDISTANCE;
			double minSelY = Math.min(startDrag.getY(), endDrag.getY())-EXACTSELECTDISTANCE;
			double maxSelY = Math.max(startDrag.getY(), endDrag.getY())+EXACTSELECTDISTANCE;
			Point2D.Double start = screenToDatabase((int)minSelX, (int)minSelY);
			Point2D.Double end = screenToDatabase((int)maxSelX, (int)maxSelY);
			Rectangle2D.Double searchArea = new Rectangle2D.Double(Math.min(start.getX(), end.getX()),
				Math.min(start.getY(), end.getY()), Math.abs(start.getX() - end.getX()), Math.abs(start.getY() - end.getY()));
			Geometric.Search sea = new Geometric.Search(searchArea, cell);
			for(;;)
			{
				Geometric nextGeom = sea.nextObject();
				if (nextGeom == null) break;
				addHighlighting(nextGeom);
			}
			doingDrag = false;
			repaint();
		}
	}

	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseMoved(MouseEvent evt) {}

	public void mouseDragged(MouseEvent evt)
	{
		int newX = evt.getX();
		int newY = evt.getY();
		if (doingDrag)
		{
			endDrag.setLocation(newX, newY);
			repaint();
			return;
		}
		if ((evt.getModifiers()&MouseEvent.CTRL_MASK) != 0)
		{
			// control key held: zoom
			scale = scale * Math.exp((oldy-newY) / 100.0f);
		} else if ((evt.getModifiers()&MouseEvent.SHIFT_MASK) != 0)
		{
			// shift key held: pan
			offx -= (newX - oldx) / scale;
			offy += (newY - oldy) / scale;
		}
		oldx = newX;
		oldy = newY;
		redraw();
	}

	public void keyPressed(KeyEvent e)
	{
	}

	public void keyReleased(KeyEvent e)
	{
	}

	public void keyTyped(KeyEvent e)
	{
		if (e.getKeyChar() == 'f')
		{
			System.out.println("doing full display...");
			UserMenuCommands.fullDisplayCommand();
			System.out.println("...did full display");
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int clicks = e.getWheelRotation();
		System.out.println("Mouse wheel rolled by " + clicks);
	}

}

