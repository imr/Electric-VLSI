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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.user.UserMenuCommands;

import javax.swing.JPanel;
import java.awt.event.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.awt.font.*;
import java.util.List;
import java.util.ArrayList;

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
	/** the offscreen image of the window */				private Image img = null;
	/** true if the window needs to be rerendered */		private boolean needsUpdate = false;
	/** true to track the time for redraw */				private boolean trackTime = false;

	/** an identity transformation */						private static final AffineTransform IDENTITY = new AffineTransform();
	/** the offset of each new window on the screen */		private static int windowOffset = 0;
	/** the offset of each new window on the screen */		private static List windowList = new ArrayList();

	/** for drawing solid lines */		private static final BasicStroke solidLine = new BasicStroke(0);
	/** for drawing thick lines */		private static final BasicStroke thickLine = new BasicStroke(1);
	/** for drawing dotted lines */		private static final BasicStroke dottedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1}, 0);
	/** for drawing dashed lines */		private static final BasicStroke dashedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {10}, 0);

	// constructor
	private UIEdit(Cell cell)
	{
		//super(cell.describe(), true, true, true, true);
		this.cell = cell;

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
		windowList.add(ui);
		return ui;
	}

	public void redraw()
	{
		needsUpdate = true;
		repaint();
	}

	public void setTimeTracking(boolean trackTime)
	{
		this.trackTime = trackTime;
	}

	/**
	 * Routine to set the cell that is shown in the window to "cell".
	 */
	public void setCell(Cell cell)
	{
		this.cell = cell;
		fillScreen();
		redraw();
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
			drawArc(g2, (ArcInst)arcs.next(), prevTrans);
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

		// debug to show outline
//		if (topLevel)
//		{
//			Layer outlineLayer = Layer.newInstance("Outline",
//				new EGraphics(EGraphics.LAYERO, EGraphics.BLACK, EGraphics.SOLIDC, EGraphics.SOLIDC, 255,255,255,1,1,
//				new int[] {0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff,
//					0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff, 0xffff}));
//			Rectangle2D rect = ni.getBounds();
//			Poly [] polys = new Poly[1];
//			Point2D.Double [] points = new Point2D.Double[4];
//			points[0] = new Point2D.Double(rect.getMinX(), rect.getMinY());
//			points[1] = new Point2D.Double(rect.getMaxX(), rect.getMinY());
//			points[2] = new Point2D.Double(rect.getMaxX(), rect.getMaxY());
//			points[3] = new Point2D.Double(rect.getMinX(), rect.getMaxY());
//			polys[0] = new Poly(points);
//			polys[0].setStyle(Poly.Type.CLOSED);
//			polys[0].setLayer(outlineLayer);
//			drawPolys(g2, polys, IDENTITY);
//		}

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
			}
		} else
		{
			// primitive
			if (topLevel || !ni.isVisInside())
			{
				PrimitiveNode prim = (PrimitiveNode)np;
				Technology tech = prim.getTechnology();
				Poly [] polys = tech.getShape(ni);
				drawPolys(g2, polys, localTrans);
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
	void drawArc(Graphics2D g2, ArcInst ai, AffineTransform trans)
	{
		ArcProto ap = ai.getProto();
		Technology tech = ap.getTechnology();
		Poly [] polys = tech.getShape(ai);
		drawPolys(g2, polys, trans);
	}

	/**
	 * Routine to draw polygon "poly", transformed through "trans".
	 */
	void drawPolys(Graphics2D g2, Poly [] polys, AffineTransform trans)
	{
		if (polys == null) return;
		for(int i = 0; i < polys.length; i++)
		{
			// get the polygon and transform it
			Poly poly = polys[i];
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
				int radius = (int)(EMath.computeDistance(points[0], points[1]) * scale);
				int diameter = radius * 2;
				int startAngle = (int)(Math.atan2(startY-ctrY, startX-ctrX) * 180.0 / Math.PI);
				if (startAngle < 0) startAngle += 360;
				int endAngle = (int)(Math.atan2(endY-ctrY, endX-ctrX) * 180.0 / Math.PI);
				if (endAngle < 0) endAngle += 360;
				if (startAngle < endAngle) startAngle += 360;
				startAngle = 360 - startAngle;
				endAngle = 360 - endAngle;
				g2.drawArc(ctrX-radius, ctrY-radius, diameter, diameter, endAngle, startAngle - endAngle);
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
	}

	/**
	 * Routine to draw a large or small cross, as described in "poly".
	 */
	void drawCross(Graphics2D g2, Poly poly)
	{
		g2.setStroke(solidLine);
		float x = (float)poly.getCenterX();
		float y = (float)poly.getCenterY();
		g2.setColor(Color.black);
		GeneralPath gp = new GeneralPath();
		Poly.Type theStyle = poly.getStyle();
		float size;
		if (theStyle == Poly.Type.CROSS) size = 3 / (float)scale; else
			size = 5 / (float)scale;
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
				gp.lineTo((float)(points[j].getX()*theScale), (float)(points[j].getY()*theScale));
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

//		/** text at center */								public static final Type TEXTCENT=       new Type();
//		/** text below top edge */							public static final Type TEXTTOP=        new Type();
//		/** text above bottom edge */						public static final Type TEXTBOT=        new Type();
//		/** text to right of left edge */					public static final Type TEXTLEFT=       new Type();
//		/** text to left of right edge */					public static final Type TEXTRIGHT=      new Type();
//		/** text to lower-right of top-left corner */		public static final Type TEXTTOPLEFT=    new Type();
//		/** text to upper-right of bottom-left corner */	public static final Type TEXTBOTLEFT=    new Type();
//		/** text to lower-left of top-right corner */		public static final Type TEXTTOPRIGHT=   new Type();
//		/** text to upper-left of bottom-right corner */	public static final Type TEXTBOTRIGHT=   new Type();
//		/** text that fits in box (may shrink) */			public static final Type TEXTBOX=        new Type();

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
		//super.paint(g);
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

	int oldx, oldy;

	public void mousePressed(MouseEvent evt)
	{
		oldx = evt.getX();
		oldy = evt.getY();
	}

	public void mouseReleased(MouseEvent evt) {}
	public void mouseClicked(MouseEvent evt) {}
	public void mouseEntered(MouseEvent evt) {}
	public void mouseExited(MouseEvent evt) {}
	public void mouseMoved(MouseEvent evt) {}

	public void mouseDragged(MouseEvent evt)
	{
		if ((evt.getModifiers()&evt.CTRL_MASK) != 0)
		{
			// control key held: zoom
			scale = scale * Math.exp((oldy-evt.getY()) / 100.0f);
		} else
		{
			// shift key held: pan
			offx -= (evt.getX() - oldx) / scale;
			offy += (evt.getY() - oldy) / scale;
		}
		oldx = evt.getX();
		oldy = evt.getY();
		needsUpdate = true;
		repaint();
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
			System.out.println("doing full display");
			UserMenuCommands.fullDisplayCommand();
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		int clicks = e.getWheelRotation();
		System.out.println("Mouse wheel rolled by " + clicks);
	}

}

