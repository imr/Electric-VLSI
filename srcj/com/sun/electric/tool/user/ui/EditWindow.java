/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EditWindow.java
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
import com.sun.electric.database.hierarchy.Nodable;
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
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.UserMenuCommands;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.ToolBar;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Stack;
import java.util.EventListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import javax.swing.KeyStroke;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Arc2D;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/*
 * This class defines an editing window for circuitry.
 */

public class EditWindow extends JPanel
	implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ActionListener
{
    /** the window scale */									private double scale;
    /** the window offset */								private double offx, offy;
    /** the size of the window (in pixels) */				private Dimension sz;
    /** the cell that is in the window */					private Cell cell;
    /** Cell's VarContext */                                private VarContext cellVarContext;
    /** the offscreen image of the window */				private Image img = null;
    /** the window frame containing this editwindow */      private WindowFrame wf;
	/** true if the window needs to be rerendered */		private boolean needsUpdate = false;
	/** true to track the time for redraw */				private boolean trackTime = false;
	/** true if showing grid in this window */				private boolean showGrid = false;
	/** true if doing object-selection drag */				private boolean doingAreaDrag = false;
	/** starting screen point for drags in this window */	private Point startDrag = new Point();
	/** ending screen point for drags in this window */		private Point endDrag = new Point();
	/** current mouse listener */							private static MouseListener curMouseListener = ClickAndDragListener.theOne;
	/** current mouse motion listener */					private static MouseMotionListener curMouseMotionListener = ClickAndDragListener.theOne;
	/** current mouse wheel listener */						private static MouseWheelListener curMouseWheelListener = ClickAndDragListener.theOne;
	/** current key listener */								private static KeyListener curKeyListener = ClickAndDragListener.theOne;

    /** an identity transformation */						private static final AffineTransform IDENTITY = new AffineTransform();
    /** the offset of each new window on the screen */		private static int windowOffset = 0;

    /** for drawing solid lines */		private static final BasicStroke solidLine = new BasicStroke(0);
    /** for drawing thick lines */		private static final BasicStroke thickLine = new BasicStroke(1);
    /** for drawing dotted lines */		private static final BasicStroke dottedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1}, 0);
    /** for drawing dashed lines */		private static final BasicStroke dashedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {10}, 0);
    /** for drawing selection boxes */	private static final BasicStroke selectionLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {2}, 3);


    // ************************************* CONTROL *************************************

    // constructor
    private EditWindow(Cell cell, WindowFrame wf)
	{
        //super(cell.describe(), true, true, true, true);
        this.cell = cell;
        this.cellVarContext = VarContext.globalContext;
        this.wf = wf;
        
		sz = new Dimension(500, 500);
		setSize(sz.width, sz.height);
		setPreferredSize(sz);
		//setAutoscrolls(true);
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	}

	// factory
	public static EditWindow CreateElectricDoc(Cell cell, WindowFrame wf)
	{
		EditWindow ui = new EditWindow(cell, wf);
		return ui;
	}

	public static EditWindow findWindow(Cell cell)
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			EditWindow wnd = wf.getEditWindow();
			if (wnd.getCell() == cell) return wnd;
		}
		return null;
	}

    // ************************************* RENDERING A WINDOW *************************************

	public void redraw()
	{
		needsUpdate = true;
		repaint();
	}

	public static void redrawAll()
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			EditWindow wnd = wf.getEditWindow();
			wnd.needsUpdate = true;
			wnd.repaint();
		}
	}

	public void paint(Graphics g)
	{
        Graphics2D g2 = (Graphics2D)g;

		// to enable keys to be recieved
		requestFocus();
        
		if (img == null || !getSize().equals(sz))
		{
			sz = getSize();
			img = createImage(sz.width, sz.height);
			fillScreen();
		}
		if (needsUpdate)
		{
			needsUpdate = false;
			drawImage();
		}
		g2.drawImage(img, 0, 0, this);

        if (cell == null) return;

        // add in grid
		if (showGrid) drawGrid(g2);

		// add in highlighting
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			Cell highCell = h.getCell();
			if (highCell != cell) continue;
			h.showHighlight(this, g2);
		}

		// add in drag area
		if (doingAreaDrag)
			showDragBox(g2);
	}

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

		// show cell variables if at the top level
		if (topLevel)
		{
			// show displayable variables on the instance
			int numPolys = cell.numDisplayableVariables(true);
			Poly [] polys = new Poly[numPolys];
			Rectangle2D rect = cell.getBounds();
			cell.addDisplayableVariables(rect, polys, 0, this, true);
			drawPolys(g2, polys, new AffineTransform());
		}
	}

	/**
	 * Routine to draw node "ni", transformed through "trans".
	 */
	public void drawNode(Graphics2D g2, NodeInst ni, AffineTransform trans, boolean topLevel)
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
				TextDescriptor descript = ni.getProtoTextDescriptor();
				drawText(g2, bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY(),
					Poly.Type.TEXTBOX, descript, np.describe(), Color.black);

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
					descript = portPoly.getTextDescriptor();
					drawText(g2, portPoly.getCenterX(), portPoly.getCenterX(), portPoly.getCenterY(), portPoly.getCenterY(), Poly.Type.TEXTCENT,
						descript, portlist[i].getPortProto().getProtoName(), Color.red);
				}
			}

			// draw any displayable variables on the instance
			int numPolys = ni.numDisplayableVariables(true);
			Poly [] polys = new Poly[numPolys];
			Rectangle2D rect = ni.getBounds();
			ni.addDisplayableVariables(rect, polys, 0, this, true);
			if (drawPolys(g2, polys, localTrans))
				System.out.println("... while displaying instance "+ni.describe() + " in cell " + ni.getParent().describe());
		} else
		{
			// primitive
			if (topLevel || !ni.isVisInside())
			{
				PrimitiveNode prim = (PrimitiveNode)np;
				Technology tech = prim.getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, this);
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
				Poly poly = e.getNamePoly();
				Rectangle2D rect = (Rectangle2D)poly.getBounds2D().clone();
				poly.transform(localTrans);
				TextDescriptor descript = poly.getTextDescriptor();
				drawText(g2, poly.getCenterX(), poly.getCenterX(), poly.getCenterY(), poly.getCenterY(), Poly.Type.TEXTCENT, descript, e.getProtoName(),
					Color.black);

				// draw variables on the export
				int numPolys = e.numDisplayableVariables(true);
				if (numPolys > 0)
				{
					Poly [] polys = new Poly[numPolys];
					e.addDisplayableVariables(rect, polys, 0, this, true);
					if (drawPolys(g2, polys, localTrans))
						System.out.println("... while displaying export in cell " + ni.getParent().describe());
				}
			}
		}
	}

	/**
	 * Routine to draw arc "ai", transformed through "trans".
	 */
	public void drawArc(Graphics2D g2, ArcInst ai, AffineTransform trans, boolean topLevel)
	{
		ArcProto ap = ai.getProto();
		Technology tech = ap.getTechnology();
		Poly [] polys = tech.getShapeOfArc(ai, this);
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
				double lX = poly.getCenterX();
				double hX = lX;
				double lY = poly.getCenterY();
				double hY = lY;
				if (style == Poly.Type.TEXTBOX)
				{
					Rectangle2D bounds = poly.getBounds2D();
					lX = bounds.getMinX();   hX = bounds.getMaxX();
					lY = bounds.getMinY();   hY = bounds.getMaxY();
				}
				TextDescriptor descript = poly.getTextDescriptor();
				String str = poly.getString();
				drawText(g2, lX, hX, lY, hY, style, descript, str, Color.black);
			} else if (style == Poly.Type.CIRCLE || style == Poly.Type.THICKCIRCLE || style == Poly.Type.DISC)
			{
				drawCircular(g2, poly);
			} else if (style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
			{
				AffineTransform saveAT = g2.getTransform();
				double scale = 100;
				g2.scale(1/scale, 1/scale);
				Point2D [] points = poly.getPoints();
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
		g2.scale(1/cScale, 1/cScale);
		if (poly.getStyle() == Poly.Type.THICKCIRCLE)
		{
			g2.setStroke(thickLine);
		} else
		{
			g2.setStroke(solidLine);
		}
		Point2D [] points = poly.getPoints();
		double ctrX = points[0].getX() * cScale;
		double ctrY = points[0].getY() * cScale;
		double edgeX = points[1].getX() * cScale;
		double edgeY = points[1].getY() * cScale;
		double radius;
		if (edgeX == ctrX) radius = Math.abs(ctrY - edgeY); else
			if (edgeY == ctrY) radius = Math.abs(ctrX - edgeX); else
				radius = Math.sqrt((ctrY - edgeY)*(ctrY - edgeY) + (ctrX - edgeX) * (ctrX - edgeX));
		double diameter = radius * 2;
        Arc2D circle = new Arc2D.Double((int)ctrX-radius, (int)ctrY-radius, (int)diameter, (int)diameter,
                                        (int)0.0, (int)360.0, Arc2D.OPEN);
        if (poly.getStyle() == Poly.Type.DISC)
		{
            g2.fill(circle);
			//g2.fillOval(ctrX-radius, ctrY-radius, diameter, diameter);
		} else
		{
            g2.draw(circle);
            //g2.drawOval(ctrX-radius, ctrY-radius, diameter, diameter);
		}
		g2.setTransform(saveAT);
	}

	/**
	 * Routine to convert a string and descriptor to a GlyphVector.
	 * @param text the string to convert.
	 * @param descript the Text Descriptor, with size, style, etc.
	 * @return a GlyphVector describing the text.
	 */
	public GlyphVector getGlyphs(String text, TextDescriptor descript)
	{
		// make a glyph vector for the desired text
		int size = 14;
		int fontStyle = Font.PLAIN;
		if (descript != null)
		{
			size = descript.getTrueSize(this);
			if (size <= 0) size = 1;
			if (descript.isItalic()) fontStyle |= Font.ITALIC;
			if (descript.isBold()) fontStyle |= Font.BOLD;
		}
		Font font = new Font("SansSerif", fontStyle, size);
		FontRenderContext frc = new FontRenderContext(null, false, false);
		GlyphVector gv = font.createGlyphVector(frc, text);
		return gv;
	}

	/**
	 * Routine to return the coordinates of the lower-left corner of text in this window.
	 * @param gv the GlyphVector describing the text.
	 * @param style the grab-point information about where the text is anchored.
	 * @param lX the low X bound of the polygon containing the text.
	 * @param hX the high X bound of the polygon containing the text.
	 * @param lY the low Y bound of the polygon containing the text.
	 * @param hY the high Y bound of the polygon containing the text.
	 * @return the coordinates of the lower-left corner of the text.
	 */
	public Point2D getTextCorner(GlyphVector gv, Poly.Type style, int numLines, double lX, double hX, double lY, double hY)
	{
		// adjust to place text in the center
		Rectangle2D glyphBounds = gv.getVisualBounds();
		double textScale = 1.0/scale;
		double cX = (lX + hX) / 2;
		double cY = (lY + hY) / 2;
		double textWidth = glyphBounds.getWidth() * textScale;
		double textHeight = glyphBounds.getHeight() * textScale;
		if (style == Poly.Type.TEXTCENT)
		{
			cX -= glyphBounds.getCenterX() * textScale;
			cY += glyphBounds.getCenterY() * textScale;
		} else if (style == Poly.Type.TEXTTOP)
		{
			cX -= glyphBounds.getCenterX() * textScale;
			cY -= textHeight;
		} else if (style == Poly.Type.TEXTBOT)
		{
			cX -= glyphBounds.getCenterX() * textScale;
		} else if (style == Poly.Type.TEXTLEFT)
		{
			cY += glyphBounds.getCenterY() * textScale;
		} else if (style == Poly.Type.TEXTRIGHT)
		{
			cX -= textWidth;
			cY += glyphBounds.getCenterY() * textScale;
		} else if (style == Poly.Type.TEXTTOPLEFT)
		{
			cY -= textHeight;
		} else if (style == Poly.Type.TEXTBOTLEFT)
		{
		} else if (style == Poly.Type.TEXTTOPRIGHT)
		{
			cX -= textWidth;
			cY -= textHeight;
		} else if (style == Poly.Type.TEXTBOTRIGHT)
		{
			cX -= textWidth;
		} if (style == Poly.Type.TEXTBOX)
		{
			if (textWidth > hX - lX)
			{
				// text too big for box: scale it down
				textScale *= (hX - lX) / textWidth;
			}
			cX -= glyphBounds.getCenterX() * textScale;
			cY += glyphBounds.getCenterY() * textScale;
		}
		return new Point2D.Double(cX, cY);
	}

	/**
	 * Routine to return the scaling factor between database and screen for the given text.
	 * @param gv the GlyphVector describing the text.
	 * @param style the grab-point information about where the text is anchored.
	 * @param lX the low X bound of the polygon containing the text.
	 * @param hX the high X bound of the polygon containing the text.
	 * @param lY the low Y bound of the polygon containing the text.
	 * @param hY the high Y bound of the polygon containing the text.
	 * @return the scale of the text (from database to screen).
	 */
	public double getTextScale(GlyphVector gv, Poly.Type style, double lX, double hX, double lY, double hY)
	{
		double textScale = 1.0/scale;
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
	 * Routine to draw text on the window.
	 * @param g2 the Graphics object for drawing.
	 * @param lX the low X bound of the polygon containing the text.
	 * @param hX the high X bound of the polygon containing the text.
	 * @param lY the low Y bound of the polygon containing the text.
	 * @param hY the high Y bound of the polygon containing the text.
	 * @param style the grab-point information about where the text is anchored.
	 * @param descript the text descriptor for the text.
	 * @param text the text to be drawn.
	 * @param color the color to draw the text.
	 */
	void drawText(Graphics2D g2, double lX, double hX, double lY, double hY, Poly.Type style, TextDescriptor descript, String text, Color color)
	{
		GlyphVector gv = getGlyphs(text, descript);
		Rectangle2D glyphBounds = gv.getVisualBounds();
		Point2D corner = getTextCorner(gv, style, 1, lX, hX, lY, hY);
		double cX = corner.getX();
		double cY = corner.getY();

		// determine scaling for the window
		double textScale = getTextScale(gv, style, lX, hX, lY, hY);

		// draw the text
		AffineTransform saveAT = g2.getTransform();
		g2.translate(cX, cY);
		g2.scale(textScale, -textScale);
		g2.translate(-cX, -cY);
		g2.setColor(color);
		g2.drawGlyphVector(gv, (float)cX, (float)cY);
		if (descript != null && descript.isUnderline())
		{
			GeneralPath gp = new GeneralPath();
			gp.moveTo((float)cX, (float)cY);
			gp.lineTo((float)(cX+glyphBounds.getWidth()), (float)cY);
			g2.draw(gp);
		}
		g2.setTransform(saveAT);
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
		Point2D low = screenToDatabase(0, 0);
		double x4 = low.getX();  double y4 = low.getY();
		Point2D high = screenToDatabase(sz.width, sz.height);
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

	// ************************************* WINDOW ZOOM AND PAN *************************************

	/**
	 * Routine to return the scale factor for this window.
	 * @return the scale factor for this window.
	 */
	public double getScale() { return scale; }

	/**
	 * Routine to set the scale factor for this window.
	 * @param scale the scale factor for this window.
	 */
	public void setScale(double scale) { this.scale = scale; }

	/**
	 * Routine to return the offset factor for this window.
	 * @return the offset factor for this window.
	 */
	public Point2D getOffset() { return new Point2D.Double(offx, offy); }

	/**
	 * Routine to set the offset factor for this window.
	 * @param off the offset factor for this window.
	 */
	public void setOffset(Point2D off) { offx = off.getX();   offy = off.getY(); }

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

    // ************************************* HIERARCHY TRAVERSAL *************************************

    /**
     * Get the window's VarContext
     * @return the current VarContext
     */
    public VarContext getVarContext() { return cellVarContext; }

    /** 
     * Push into an instance (go down the hierarchy)
     */
    public void downHierarchy() {
        if (Highlight.getNumHighlights() <= 0) {
            System.out.println("Nothing highlighted, cannot descend");
            return;
        }
        if (Highlight.getNumHighlights() > 1) {
            System.out.println("More than one thing highlighted, cannot descend");
            return;
        }
        Iterator it = Highlight.getHighlights();
        Highlight h = (Highlight)it.next();
		if (h.getType() != Highlight.Type.GEOM) {
            System.out.println("Must select a node");
            return;
        }
        Geometric geom = h.getGeom();
        if (!(geom instanceof NodeInst)) {
            System.out.println("Cannot descend into that object");
            return;
        }
        NodeInst ni = (NodeInst)geom;
        NodeProto np = ni.getProto();
        if (!(np instanceof Cell)) {
            System.out.println("Cannot descend into that object");
            return;
        }
        Cell cell = (Cell)np;
        Cell schCell = cell.getEquivalent();
        // special case: if cell is icon of current cell, descend into icon
        if (this.cell == schCell) schCell = cell;
        if (schCell == null) return;                // nothing to descend into
        setCell(schCell, cellVarContext.push(ni));
    }

    /**
     * Pop out of an instance (go up the hierarchy)
     */
    public void upHierarchy() {
        try {
            Nodable ni = cellVarContext.getNodable();
            Cell parent = ni.getParent();
            VarContext context = cellVarContext.pop();
            setCell(parent, context);
			if (ni instanceof NodeInst)
				Highlight.addGeometric((NodeInst)ni);
            needsUpdate = true;
        } catch (NullPointerException e) {
            // no parent - if icon, go to sch view
            // - otherwise, ask user where to go if necessary
            if (cell.getView() == View.ICON) {
                Cell schCell = cell.getEquivalent();
                if (schCell == null) return;        // nothing to do
                setCell(schCell, VarContext.globalContext);
                return;
            }            
            JPopupMenu parents = new JPopupMenu("parents");
            // find all possible parents in all libraries
            HashSet found = new HashSet();
            for (Iterator libIt = Library.getLibraries(); libIt.hasNext();) {
                Library lib = (Library)libIt.next();
                System.out.println("checking library "+lib);
                for (Iterator cellIt = lib.getCells(); cellIt.hasNext();) {
                    Cell cel = (Cell)cellIt.next();
                    if (cel == this.cell) continue; // don't add myself
                    for (Iterator instIt = cel.getNodes(); instIt.hasNext();) {
                        NodeInst inst = (NodeInst)instIt.next();  // get all instances in this cell
                        NodeProto np = inst.getProto(); // get prototype
                        if (!(np instanceof Cell)) continue;
                        Cell npCell = (Cell)np;
                        // see if same cell group (but alternate view)
                        String libcellname = cel.describe();
                        if (this.cell.getCellGroup().containsCell(npCell) &&
                            !found.contains(libcellname)) {
                            found.add(libcellname); // for multiple instances in same cell
                            JMenuItem menuItem = new JMenuItem(libcellname);
                            menuItem.addActionListener(this);
                            parents.add(menuItem);
                        }
                    }
                }
            }
            if (parents.getComponentCount() > 0)
                parents.show(this, 0, 0);
            else {
                parents.add(new JMenuItem("not instantiated anywhere"));
                parents.show(this, 0, 0);
            }
        }
    }

    /** 
     * Respond to an action performed, in this case change the current cell
     * when the user clicks on an entry in the upHierarchy popup menu.
     */
    public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem)e.getSource();
        // extract library and cell from string
        Cell cell = Cell.getCellFromDescription(source.getText());
        if (cell == null) return;
        setCell(cell, VarContext.globalContext);
    }

	// ************************************* COORDINATES *************************************

	public Point2D screenToDatabase(int screenX, int screenY)
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

	public Point2D deltaScreenToDatabase(int screenDX, int screenDY)
	{
		double dbDX = screenDX / scale;
		double dbDY = (-screenDY) / scale;
		return new Point2D.Double(dbDX, dbDY);
	}

	public Point deltaDatabaseToScreen(double dbDX, double dbDY)
	{
		int screenDX = (int)Math.round(dbDX * scale);
		int screenDY = (int)Math.round(-dbDY * scale);
		return new Point(screenDX, screenDY);
	}

	/**
	 * Routine to snap a point to the nearest database-space grid unit.
	 * @param pt the point to be snapped.
	 * @param alignment the size of the snap grid (1 to round to whole numbers)
	 */
	public static void gridAlign(Point2D pt, double alignment)
	{
		long x = Math.round(pt.getX() / alignment);
		long y = Math.round(pt.getY() / alignment);
		pt.setLocation(x * alignment, y * alignment);
	}

	// ************************************* EVENT LISTENERS *************************************
	
	public static void setListener(EventListener listener)
	{
		curMouseListener = (MouseListener)listener;
		curMouseMotionListener = (MouseMotionListener)listener;
		curMouseWheelListener = (MouseWheelListener)listener;
		curKeyListener = (KeyListener)listener;
	}

	public static EventListener getListener() { return curMouseListener; }

	// the MouseListener events
	public void mousePressed(MouseEvent evt) { curMouseListener.mousePressed(evt); }
	public void mouseReleased(MouseEvent evt) { curMouseListener.mouseReleased(evt); }
	public void mouseClicked(MouseEvent evt) { curMouseListener.mouseClicked(evt); }
	public void mouseEntered(MouseEvent evt) { curMouseListener.mouseEntered(evt); }
	public void mouseExited(MouseEvent evt) { curMouseListener.mouseExited(evt); }

	// the MouseMotionListener events
	public void mouseMoved(MouseEvent evt) { curMouseMotionListener.mouseMoved(evt); }
	public void mouseDragged(MouseEvent evt) { curMouseMotionListener.mouseDragged(evt); }

	// the MouseWheelListener events
	public void mouseWheelMoved(MouseWheelEvent evt) { curMouseWheelListener.mouseWheelMoved(evt); }

	// the KeyListener events
	public void keyPressed(KeyEvent evt)
	{
		int key = evt.getKeyCode();
		if (key == KeyEvent.VK_W) ToolBar.wiringCommand(); else
		if (key == KeyEvent.VK_M) ToolBar.selectCommand(); else
		if (key == KeyEvent.VK_P) ToolBar.panCommand(); else
		if (key == KeyEvent.VK_Z) ToolBar.zoomCommand(); else
		{
			curKeyListener.keyPressed(evt);
		}
	}
	public void keyReleased(KeyEvent evt) { curKeyListener.keyReleased(evt); }
	public void keyTyped(KeyEvent evt) { curKeyListener.keyTyped(evt); }

	// ************************************* MISCELLANEOUS *************************************

	public void setTimeTracking(boolean trackTime)
	{
		this.trackTime = trackTime;
	}

	/**
	 * Routine to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return cell; }

	/**
	 * Routine to set the cell that is shown in the window to "cell".
	 */
	public void setCell(Cell cell, VarContext context)
	{
		this.cell = cell;
        this.cellVarContext = context;
		Library curLib = Library.getCurrent();
		curLib.setCurCell(cell);
		Highlight.clear();
//        if (lastPushed != null) Highlight.addGeometric(lastPushed);
		Highlight.finished();
		fillScreen();
		redraw();
        wf.setTitle(cell.describe());
	}
	
	public boolean isDoingAreaDrag() { return doingAreaDrag; }

	public void setDoingAreaDrag() { doingAreaDrag = true; }

	public void clearDoingAreaDrag() { doingAreaDrag = false; }

	public Point getStartDrag() { return startDrag; }

	public void setStartDrag(int x, int y) { startDrag.setLocation(x, y); }

	public Point getEndDrag() { return endDrag; }

	public void setEndDrag(int x, int y) { endDrag.setLocation(x, y); }

}
