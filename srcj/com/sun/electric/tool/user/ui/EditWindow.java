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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.MenuCommands;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.PixelDrawing;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Font;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
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
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.Stack;
import java.util.EventListener;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

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

	/** the offscreen data for rendering */					private PixelDrawing offscreen = null;
    /** the window frame containing this editwindow */      private WindowFrame wf;
	/** true if the window needs to be rerendered */		private boolean needsUpdate = true;
	/** true if showing grid in this window */				private boolean showGrid = false;
	/** X spacing of grid dots in this window */			private double gridXSpacing;
	/** Y spacing of grid dots in this window */			private double gridYSpacing;
	/** true if doing object-selection drag */				private boolean doingAreaDrag = false;
	/** starting screen point for drags in this window */	private Point startDrag = new Point();
	/** ending screen point for drags in this window */		private Point endDrag = new Point();
    /** current mouse listener */							private static MouseListener curMouseListener = ClickZoomWireListener.theOne;
    /** current mouse motion listener */					private static MouseMotionListener curMouseMotionListener = ClickZoomWireListener.theOne;
    /** current mouse wheel listener */						private static MouseWheelListener curMouseWheelListener = ClickZoomWireListener.theOne;
    /** current key listener */								private static KeyListener curKeyListener = ClickZoomWireListener.theOne;

    /** the offset of each new window on the screen */		private static int windowOffset = 0;
	/** zero rectangle */									private static final Rectangle2D CENTERRECT = new Rectangle2D.Double(0, 0, 0, 0);
	/** TextDescriptor for empty window text. */			private static TextDescriptor noCellTextDescriptor = null;

    /** for drawing solid lines */		private static final BasicStroke solidLine = new BasicStroke(0);
    /** for drawing thick lines */		private static final BasicStroke thickLine = new BasicStroke(1);
    /** for drawing dotted lines */		private static final BasicStroke dottedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {1}, 0);
    /** for drawing dashed lines */		private static final BasicStroke dashedLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {10}, 0);
    /** for drawing selection boxes */	private static final BasicStroke selectionLine = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {2}, 3);
	EGraphics blackGraphics = new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 0,0,0, 1.0,1,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});
	EGraphics redGraphics = new EGraphics(EGraphics.SOLIDC, EGraphics.SOLIDC, 0, 255,0,0, 1.0,1,
		new int[] {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0});


    // ************************************* CONTROL *************************************

    // constructor
    private EditWindow(Cell cell, WindowFrame wf)
	{
        //super(cell.describe(), true, true, true, true);
        this.wf = wf;
		this.gridXSpacing = User.getDefGridXSpacing();
		this.gridYSpacing = User.getDefGridYSpacing();

		sz = new Dimension(500, 500);
		setSize(sz.width, sz.height);
		setPreferredSize(sz);
		//setAutoscrolls(true);
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		if (wf != null) setCell(cell, VarContext.globalContext);
	}

	public void setSize(Dimension sz)
	{
		this.sz = sz;
		offscreen = new PixelDrawing(sz, this);
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

	public static EditWindow getCurrent()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return null;
		return wf.getEditWindow();
	}

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return cell; }

	/**
	 * Method to set the cell that is shown in the window to "cell".
	 */
	public void setCell(Cell cell, VarContext context)
	{
		this.cell = cell;
        this.cellVarContext = context;
		Library curLib = Library.getCurrent();
		curLib.setCurCell(cell);
		Highlight.clear();
		Highlight.finished();

		// seed the offset values
//		if (cell != null)
//		{
//			Rectangle2D bounds = cell.getBounds();
//			offx = bounds.getCenterX();   offy = bounds.getCenterY();
//		}
		fillScreen();
		if (wf != null)
		{
			if (cell == null)
			{
				wf.setTitle("***NONE***");
			} else
			{
				if (wf == WindowFrame.getCurrentWindowFrame())
				{
					// if auto-switching technology, do it
					PaletteFrame.autoTechnologySwitch(cell);
				}
				wf.setTitle(cell.describe());
				if (cell.getView().isTextView())
				{
					wf.setContent(WindowFrame.TEXTWINDOW);

					// reload with text information
					JTextArea ta = wf.getTextEditWindow();
					Variable var = cell.getVar("FACET_message");
					if (var != null)
					{
						int len = var.getLength();
						String [] lines = (String [])var.getObject();
						String totalText = "";
						for(int i=0; i<len; i++)
						{
							totalText += lines[i] + "\n";
						}
						ta.setText(totalText);
						ta.setCaretPosition(0);
					}
					return;
				} else
				{
					wf.setContent(WindowFrame.DISPWINDOW);
				}
			}
		}
		redraw();

		if (cell != null && User.isCheckCellDates()) cell.checkCellDates();
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
			wnd.redraw();
		}
	}

	public void paint(Graphics g)
	{
		// to enable keys to be received
		if (cell != null && cell == Library.getCurrent().getCurCell())
			requestFocus();

		// redo the explorer tree if it changed
		ExplorerTree.rebuildExplorerTree();

		if (offscreen == null || !getSize().equals(sz))
		{
			setSize(getSize());
			needsUpdate = true;
		}
		if (needsUpdate)
		{
			needsUpdate = false;
			setScrollPosition();
//long startTime = System.currentTimeMillis();
			drawImage();
//long endTime = System.currentTimeMillis();
//System.out.println("Rendering took "+(endTime-startTime)+" milliseconds");
		}
		g.drawImage(offscreen.getImage(), 0, 0, this);

        if (cell == null) return;

        // add in grid
		if (showGrid) drawGrid(g);

		// add in highlighting
		for(Iterator it = Highlight.getHighlights(); it.hasNext(); )
		{
			Highlight h = (Highlight)it.next();
			Cell highCell = h.getCell();
			if (highCell != cell) continue;
			h.showHighlight(this, g);
		}

		// add in drag area
		if (doingAreaDrag)
			showDragBox(g);
	}

	public void initDrawing() { offscreen.clearImage(); }

	public Image termDrawing() { offscreen.composite(); return offscreen.getImage(); }

	/**
	 * Method to draw the current window.
	 */
	private void drawImage()
	{
		// initialize rendering into the offscreen image
		initDrawing();

		if (cell == null)
		{
			if (noCellTextDescriptor == null)
			{
				noCellTextDescriptor = new TextDescriptor(null);
				noCellTextDescriptor.setAbsSize(18);
				noCellTextDescriptor.setBold();
			}
			Rectangle rect = new Rectangle(sz);
			offscreen.drawText(rect, Poly.Type.TEXTBOX, noCellTextDescriptor, "No cell in this window", null, blackGraphics);
		} else
		{
			// draw everything
			drawCell(cell, EMath.MATID, true);
		}

		// merge transparent image into opaque one
		termDrawing();
	}

	/**
	 * Method to draw the contents of cell "c", transformed through "prevTrans".
	 */
	void drawCell(Cell cell, AffineTransform prevTrans, boolean topLevel)
	{
		// draw all arcs
		for(Iterator arcs = cell.getArcs(); arcs.hasNext(); )
		{
			drawArc((ArcInst)arcs.next(), prevTrans);
		}

		// draw all nodes
		for(Iterator nodes = cell.getNodes(); nodes.hasNext(); )
		{
			drawNode((NodeInst)nodes.next(), prevTrans, topLevel);
		}

		// show cell variables if at the top level
		if (topLevel && User.isTextVisibilityOnCell())
		{
			// show displayable variables on the instance
			int numPolys = cell.numDisplayableVariables(true);
			Poly [] polys = new Poly[numPolys];
			cell.addDisplayableVariables(CENTERRECT, polys, 0, this, true);
			drawPolys(polys, EMath.MATID);
		}
	}

	/**
	 * Method to draw node "ni", transformed through "trans".
	 */
	public void drawNode(NodeInst ni, AffineTransform trans, boolean topLevel)
	{
		NodeProto np = ni.getProto();
		AffineTransform localTrans = ni.rotateOut(trans);

		// see if the node is completely clipped from the screen
//		Rectangle2D clipBound = ni.getBounds();
//		Poly clipPoly = new Poly(clipBound.getCenterX(), clipBound.getCenterY(), clipBound.getWidth(), clipBound.getHeight());
//		AffineTransform screen = g2.getTransform();
//		clipPoly.transform(screen);
//		clipBound = clipPoly.getBounds();
//System.out.println("node is "+clipBound.getWidth()+"x"+clipBound.getHeight()+" at ("+clipBound.getCenterX()+","+clipBound.getCenterY());

		if (np instanceof Cell)
		{
			// cell instance
			Cell subCell = (Cell)np;

			// two ways to draw a cell instance
			if (ni.isExpanded())
			{
				// show the contents
				AffineTransform subTrans = ni.translateOut(localTrans);
				drawCell(subCell, subTrans, false);
			} else
			{
				// draw the instance outline
				Rectangle2D bounds = ni.getBounds();
				Poly poly = new Poly(bounds.getCenterX(), bounds.getCenterY(), ni.getXSize(), ni.getYSize());
				AffineTransform localPureTrans = ni.rotateOutAboutTrueCenter(trans);
				poly.transform(localPureTrans);
				Point2D [] points = poly.getPoints();
				for(int i=0; i<points.length; i++)
				{
					int lastI = i - 1;
					if (lastI < 0) lastI = points.length - 1;
					Point from = databaseToScreen(points[lastI]);
					Point to = databaseToScreen(points[i]);
					offscreen.drawLine(from, to, null, blackGraphics, 0);
				}

				// draw the instance name
				if (User.isTextVisibilityOnInstance())
				{
					bounds = poly.getBounds2D();
					Rectangle rect = databaseToScreen(bounds);
					TextDescriptor descript = ni.getProtoTextDescriptor();
					offscreen.drawText(rect, Poly.Type.TEXTBOX, descript, np.describe(), null, blackGraphics);
				}

				// show the ports that are not further exported or connected
				FlagSet fs = PortProto.getFlagSet(1);
				for(Iterator it = ni.getProto().getPorts(); it.hasNext(); )
				{
					PortProto pp = (PortProto)it.next();
					pp.clearBit(fs);
				}
				for(Iterator it = ni.getConnections(); it.hasNext();)
				{
					Connection con = (Connection) it.next();
					PortInst pi = con.getPortInst();
					pi.getPortProto().setBit(fs);
				}
				for(Iterator it = ni.getExports(); it.hasNext();)
				{
					Export exp = (Export) it.next();
					PortInst pi = exp.getOriginalPort();
					pi.getPortProto().setBit(fs);
				}
				int portDisplayLevel = User.getPortDisplayLevel();
				for(Iterator it = ni.getProto().getPorts(); it.hasNext(); )
				{
					PortProto pp = (PortProto)it.next();
					if (pp.isBit(fs)) continue;

					Poly portPoly = ni.getShapeOfPort(pp);
					if (portPoly == null) continue;
					portPoly.transform(trans);
					if (portDisplayLevel == 2)
					{
						// draw port as a cross
						offscreen.drawCross(portPoly, redGraphics);
					} else
					{
						// draw port as text
						if (User.isTextVisibilityOnPort())
						{
							TextDescriptor descript = portPoly.getTextDescriptor();
							Poly.Type type = descript.getPos().getPolyType();
							String portName = pp.getProtoName();
							if (portDisplayLevel == 1)
							{
								// use shorter port name
								portName = pp.getShortProtoName();
							}
							Point pt = databaseToScreen(portPoly.getCenterX(), portPoly.getCenterY());
							Rectangle rect = new Rectangle(pt);
							offscreen.drawText(rect, type, descript, portName, null, redGraphics);
						}
					}
				}
				fs.freeFlagSet();
			}

			// draw any displayable variables on the instance
			if (User.isTextVisibilityOnNode())
			{
				int numPolys = ni.numDisplayableVariables(true);
				Poly [] polys = new Poly[numPolys];
				Rectangle2D rect = ni.getBounds();
				ni.addDisplayableVariables(rect, polys, 0, this, true);
				drawPolys(polys, localTrans);
			}
		} else
		{
			// primitive
			if (topLevel || !ni.isVisInside())
			{
				EditWindow wnd = this;
				PrimitiveNode prim = (PrimitiveNode)np;
				if (!User.isTextVisibilityOnNode()) wnd = null;
				if (prim == Generic.tech.invisiblePinNode)
				{
					if (!User.isTextVisibilityOnAnnotation()) wnd = null;
				}
				Technology tech = prim.getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, wnd);
				drawPolys(polys, localTrans);
			}
		}

		// draw any exports from the node
		if (topLevel && User.isTextVisibilityOnExport())
		{
			int exportDisplayLevel = User.getExportDisplayLevel();
			Iterator it = ni.getExports();
			while (it.hasNext())
			{
				Export e = (Export)it.next();
				Poly poly = e.getNamePoly();
				Rectangle2D rect = (Rectangle2D)poly.getBounds2D().clone();
				if (exportDisplayLevel == 2)
				{
					// draw port as a cross
					offscreen.drawCross(poly, blackGraphics);
				} else
				{
					// draw port as text
					TextDescriptor descript = poly.getTextDescriptor();
					Poly.Type type = descript.getPos().getPolyType();
					String portName = e.getProtoName();
					if (exportDisplayLevel == 1)
					{
						// use shorter port name
						portName = e.getShortProtoName();
					}
					Point pt = databaseToScreen(poly.getCenterX(), poly.getCenterY());
					Rectangle textRect = new Rectangle(pt);
					offscreen.drawText(textRect, type, descript, portName, null, blackGraphics);
				}

				// draw variables on the export
				int numPolys = e.numDisplayableVariables(true);
				if (numPolys > 0)
				{
					Poly [] polys = new Poly[numPolys];
					e.addDisplayableVariables(rect, polys, 0, this, true);
					drawPolys(polys, localTrans);
				}
			}
		}
	}

	/**
	 * Method to draw arc "ai", transformed through "trans".
	 */
	public void drawArc(ArcInst ai, AffineTransform trans)
	{
		ArcProto ap = ai.getProto();
		Technology tech = ap.getTechnology();
		EditWindow wnd = this;
		if (!User.isTextVisibilityOnArc()) wnd = null;
		Poly [] polys = tech.getShapeOfArc(ai, wnd);
		drawPolys(polys, trans);
	}

	/**
	 * Method to draw polygon "poly", transformed through "trans".
	 */
	private void drawPolys(Poly [] polys, AffineTransform trans)
	{
		if (polys == null) return;
		for(int i = 0; i < polys.length; i++)
		{
			// get the polygon and transform it
			Poly poly = polys[i];
			if (poly == null)
			{
				System.out.println("Warning: poly " + i + " of list of " + polys.length + " is null");
				return;
			}
			Layer layer = poly.getLayer();
			EGraphics graphics = null;
			if (layer != null)
			{
				if (!layer.isVisible()) continue;
				graphics = layer.getGraphics();
			}

			// transform the bounds
			poly.transform(trans);

			// render the polygon
			offscreen.renderPoly(poly, graphics);
		}
	}

	/**
	 * Method to convert a string and descriptor to a GlyphVector.
	 * @param text the string to convert.
	 * @param descript the Text Descriptor, with size, style, etc.
	 * @return a GlyphVector describing the text.
	 */
	public GlyphVector getGlyphs(String text, TextDescriptor descript)
	{
		// make a glyph vector for the desired text
		int size = 14;
		int fontStyle = Font.PLAIN;
		String fontName = "SansSerif";
		if (descript != null)
		{
			size = descript.getTrueSize(this);
			if (size <= 0) size = 1;
			if (descript.isItalic()) fontStyle |= Font.ITALIC;
			if (descript.isBold()) fontStyle |= Font.BOLD;
			int fontIndex = descript.getFace();
			if (fontIndex != 0)
			{
				TextDescriptor.ActiveFont af = TextDescriptor.ActiveFont.findActiveFont(fontIndex);
				if (af != null) fontName = af.getName();
			}
		}
		Font font = new Font(fontName, fontStyle, size);
		FontRenderContext frc = new FontRenderContext(null, false, false);
		GlyphVector gv = font.createGlyphVector(frc, text);
		return gv;
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
	 * Method to set the display of a grid in this window.
	 * @param showGrid true to show the grid.
	 */
	public void setGrid(boolean showGrid)
	{
		this.showGrid = showGrid;
		redraw();
	}

	/**
	 * Method to return the state of grid display in this window.
	 * @return true if the grid is displayed in this window.
	 */
	public boolean isGrid() { return showGrid; }

	/**
	 * Method to return the distance between grid dots in the X direction.
	 * @return the distance between grid dots in the X direction.
	 */
	public double getGridXSpacing() { return gridXSpacing; }
	/**
	 * Method to set the distance between grid dots in the X direction.
	 * @param spacing the distance between grid dots in the X direction.
	 */
	public void setGridXSpacing(double spacing) { gridXSpacing = spacing; }

	/**
	 * Method to return the distance between grid dots in the Y direction.
	 * @return the distance between grid dots in the Y direction.
	 */
	public double getGridYSpacing() { return gridYSpacing; }
	/**
	 * Method to set the distance between grid dots in the Y direction.
	 * @param spacing the distance between grid dots in the Y direction.
	 */
	public void setGridYSpacing(double spacing) { gridYSpacing = spacing; }

	/**
	 * Method to return a rectangle in database coordinates that covers the viewable extent of this window.
	 * @return a rectangle that describes the viewable extent of this window (database coordinates).
	 */
	public Rectangle2D displayableBounds()
	{
		Point2D low = screenToDatabase(0, 0);
		Point2D high = screenToDatabase(sz.width-1, sz.height-1);
		Rectangle2D bounds = new Rectangle2D.Double(low.getX(), high.getY(), high.getX()-low.getX(), low.getY()-high.getY());
		return bounds;
	}

	/**
	 * Method to display the grid.
	 */
	private void drawGrid(Graphics g)
	{
		/* grid spacing */
		int x0 = (int)gridXSpacing;
		int y0 = (int)gridYSpacing;

		// bold dot spacing
		int xspacing = User.getDefGridXBoldFrequency();
		int yspacing = User.getDefGridYBoldFrequency();

		/* object space extent */
		Rectangle2D displayable = displayableBounds();
		double x4 = displayable.getMinX();  double y4 = displayable.getMaxY();
		double x5 = displayable.getMaxX();  double y5 = displayable.getMinY();

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
	 * Method to return the scale factor for this window.
	 * @return the scale factor for this window.
	 */
	public double getScale() { return scale; }

	/**
	 * Method to set the scale factor for this window.
	 * @param scale the scale factor for this window.
	 */
	public void setScale(double scale) { this.scale = scale; }

	/**
	 * Method to return the offset factor for this window.
	 * @return the offset factor for this window.
	 */
	public Point2D getOffset() { return new Point2D.Double(offx, offy); }

	/**
	 * Method to set the offset factor for this window.
	 * @param off the offset factor for this window.
	 */
	public void setOffset(Point2D off) { offx = off.getX();   offy = off.getY(); }

	/**
	 * Method called when the bottom scrollbar changes.
	 */
	public void bottomScrollChanged()
	{
		if (cell == null) return;

		Rectangle2D bounds = cell.getBounds();
		double xWidth = bounds.getWidth();
		double xCenter = bounds.getCenterX();

		JScrollBar bottom = this.wf.getBottomScrollBar();
		int xThumbPos = bottom.getValue();
		int scrollBarResolution = WindowFrame.getScrollBarResolution();
		double scaleFactor = scrollBarResolution / 4;
		int computedXThumbPos = (int)((offx - xCenter) / xWidth * scaleFactor) + scrollBarResolution/2;
		if (computedXThumbPos != xThumbPos)
		{
//System.out.println("Computed thumb = " + computedXThumbPos + " but real is " + xThumbPos+ " offx was " + offx);
			offx = (xThumbPos-scrollBarResolution/2)/scaleFactor * xWidth + xCenter;
//System.out.println("   now offx="+offx);
			redraw();
		}
	}

	/**
	 * Method called when the right scrollbar changes.
	 */
	public void rightScrollChanged()
	{
		if (cell == null) return;

		Rectangle2D bounds = cell.getBounds();
		double yHeight = bounds.getHeight();
		double yCenter = bounds.getCenterY();

		JScrollBar right = this.wf.getRightScrollBar();
		int yThumbPos = right.getValue();
		int scrollBarResolution = WindowFrame.getScrollBarResolution();
		double scaleFactor = scrollBarResolution / 4;
		int computedYThumbPos = (int)((yCenter - offy) / yHeight * scaleFactor) + scrollBarResolution/2;
		if (computedYThumbPos != yThumbPos)
		{
			offy = yCenter - (yThumbPos - scrollBarResolution/2) / scaleFactor * yHeight;
			redraw();
		}
	}

	/**
	 * Method to update the scrollbars on the sides of the edit window
	 * so they reflect the true offset of the circuit.
	 */
	public void setScrollPosition()
	{
		JScrollBar bottom = wf.getBottomScrollBar();
		JScrollBar right = wf.getRightScrollBar();
		bottom.setEnabled(cell != null);
		right.setEnabled(cell != null);
		if (cell != null)
		{
			int scrollBarResolution = WindowFrame.getScrollBarResolution();
			double scaleFactor = scrollBarResolution / 4;

			Rectangle2D bounds = cell.getBounds();
			double xWidth = bounds.getWidth();
			double xCenter = bounds.getCenterX();
			int xThumbPos = (int)((offx - xCenter) / xWidth * scaleFactor) + scrollBarResolution/2;
			bottom.setValue(xThumbPos);

			double yHeight = bounds.getHeight();
			double yCenter = bounds.getCenterY();
			int yThumbPos = (int)((yCenter - offy) / yHeight * scaleFactor) + scrollBarResolution/2;
			right.setValue(yThumbPos);
		}
	}

	/**
	 * Method to focus the screen so that an area fills it.
	 * @param bounds the area to make fill the screen.
	 */
	public void focusScreen(Rectangle2D bounds)
	{
		double width = bounds.getWidth();
		double height = bounds.getHeight();
		if (width == 0 && height == 0) width = height = 2;
		double scalex = sz.width/width * 0.9;
		double scaley = sz.height/height * 0.9;
		scale = Math.min(scalex, scaley);
		offx = bounds.getCenterX();
		offy = bounds.getCenterY();
		needsUpdate = true;
	}

	public Rectangle2D getDisplayedBounds()
	{
		Rectangle2D bounds = new Rectangle2D.Double();
		double width = sz.width/scale;
		double height = sz.height/scale;
		bounds.setRect(offx - width/2, offy - height/2, width, height);
		return bounds;
	}

	/**
	 * Method to pan and zoom the screen so that the entire cell is displayed.
	 */
	public void fillScreen()
	{
		if (cell != null)
		{
			if (!cell.getView().isTextView())
			{
				Rectangle2D cellBounds = cell.getBounds();
				if (cellBounds.getWidth() == 0 && cellBounds.getHeight() == 0)
					cellBounds = new Rectangle2D.Double(0, 0, 60, 60);
				focusScreen(cellBounds);
			}
		}
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
        NodeInst ni = (NodeInst)Highlight.getOneElectricObject(NodeInst.class);
		if (ni == null) return;
        NodeProto np = ni.getProto();
        if (!(np instanceof Cell)) {
            System.out.println("Can only descend into cell instances");
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
    public void upHierarchy()
	{
        try {
            Nodable ni = cellVarContext.getNodable();
            Cell parent = ni.getParent();
            VarContext context = cellVarContext.pop();
            setCell(parent, context);
			if (ni instanceof NodeInst)
				Highlight.addElectricObject((NodeInst)ni, parent);
        } catch (NullPointerException e)
		{
            // no parent - if icon, go to sch view
            if (cell.getView() == View.ICON)
			{
                Cell schCell = cell.getEquivalent();
                if (schCell == null) return;        // nothing to do
                setCell(schCell, VarContext.globalContext);
                return;
            }            

			// find all possible parents in all libraries
            Set found = new TreeSet();
			for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Cell parent = ni.getParent();
				found.add(parent.describe());
			}
			if (cell.getView() == View.SCHEMATIC)
			{
				Cell iconView = cell.iconView();
				if (iconView != null)
				{
					for(Iterator it = iconView.getInstancesOf(); it.hasNext(); )
					{
						NodeInst ni = (NodeInst)it.next();
						Cell parent = ni.getParent();
						found.add(parent.describe());
					}
				}
			}

			// see what was found
			if (found.size() == 0)
			{
				// no parent cell
				System.out.println("Not in any cells");
			} else if (found.size() == 1)
			{
				// just one parent cell: show it
				String cellName = (String)found.iterator().next();
				Cell parent = (Cell)NodeProto.findNodeProto(cellName);
                setCell(parent, VarContext.globalContext);
			} else
			{
				// prompt the user to choose a parent cell
				JPopupMenu parents = new JPopupMenu("parents");
				for(Iterator it = found.iterator(); it.hasNext(); )
				{
					String cellName = (String)it.next();
					JMenuItem menuItem = new JMenuItem(cellName);
					menuItem.addActionListener(this);
					parents.add(menuItem);
				}
				parents.show(this, 0, 0);
			}
        }
    }

    /** 
     * Respond to an action performed, in this case change the current cell
     * when the user clicks on an entry in the upHierarchy popup menu.
     */
    public void actionPerformed(ActionEvent e)
	{
        JMenuItem source = (JMenuItem)e.getSource();
        // extract library and cell from string
        Cell cell = (Cell)NodeProto.findNodeProto(source.getText());
        if (cell == null) return;
        setCell(cell, VarContext.globalContext);
    }

	// ************************************* COORDINATES *************************************

	/**
	 * Method to convert a screen coordinate to database coordinates.
	 * @param screenX the X coordinate (on the screen in this EditWindow).
	 * @param screenY the Y coordinate (on the screen in this EditWindow).
	 * @return the coordinate of that point in database units.
	 */
	public Point2D screenToDatabase(int screenX, int screenY)
	{
		double dbX = (screenX - sz.width/2) / scale + offx;
		double dbY = (sz.height/2 - screenY) / scale + offy;
		return new Point2D.Double(dbX, dbY);
	}

	/**
	 * Method to convert a screen distance to a database distance.
	 * @param screenDX the X coordinate change (on the screen in this EditWindow).
	 * @param screenDY the Y coordinate change (on the screen in this EditWindow).
	 * @return the distance in database units.
	 */
	public Point2D deltaScreenToDatabase(int screenDX, int screenDY)
	{
		double dbDX = screenDX / scale;
		double dbDY = (-screenDY) / scale;
		return new Point2D.Double(dbDX, dbDY);
	}


	/**
	 * Method to convert a database X coordinate to screen coordinates.
	 * @param dbX the X coordinate (in database units).
	 * @return the coordinate on the screen.
	 */
	public int databaseToScreenX(double dbX)
	{
		return (int)(sz.width/2 + (dbX - offx) * scale);
	}

	/**
	 * Method to convert a database Y coordinate to screen coordinates.
	 * @param dbY the Y coordinate (in database units).
	 * @return the coordinate on the screen.
	 */
	public int databaseToScreenY(double dbY)
	{
		return (int)(sz.height/2 - (dbY - offy) * scale);
	}

	/**
	 * Method to convert a database coordinate to screen coordinates.
	 * @param dbX the X coordinate (in database units).
	 * @param dbY the Y coordinate (in database units).
	 * @return the coordinate on the screen.
	 */
	public Point databaseToScreen(double dbX, double dbY)
	{
		return new Point(databaseToScreenX(dbX), databaseToScreenY(dbY));
	}

	/**
	 * Method to convert a database coordinate to screen coordinates.
	 * @param db the coordinate (in database units).
	 * @return the coordinate on the screen.
	 */
	public Point databaseToScreen(Point2D db)
	{
		return new Point(databaseToScreenX(db.getX()), databaseToScreenY(db.getY()));
	}

	/**
	 * Method to convert a database rectangle to screen coordinates.
	 * @param db the rectangle (in database units).
	 * @return the rectangle on the screen.
	 */
	public Rectangle databaseToScreen(Rectangle2D db)
	{
		int screenLX = (int)(sz.width/2 + (db.getMinX() - offx) * scale);
		int screenHX = (int)(sz.width/2 + (db.getMaxX() - offx) * scale);
		int screenLY = (int)(sz.height/2 - (db.getMinY() - offy) * scale);
		int screenHY = (int)(sz.height/2 - (db.getMaxY() - offy) * scale);
		if (screenHX < screenLX) { int swap = screenHX;   screenHX = screenLX; screenLX = swap; }
		if (screenHY < screenLY) { int swap = screenHY;   screenHY = screenLY; screenLY = swap; }
		return new Rectangle(screenLX, screenLY, screenHX-screenLX, screenHY-screenLY);
	}

	/**
	 * Method to convert a database distance to a screen distance.
	 * @param dbX the X change (in database units).
	 * @param dbY the Y change (in database units).
	 * @return the distance on the screen.
	 */
	public Point deltaDatabaseToScreen(double dbDX, double dbDY)
	{
		int screenDX = (int)Math.round(dbDX * scale);
		int screenDY = (int)Math.round(-dbDY * scale);
		return new Point(screenDX, screenDY);
	}

	/**
	 * Method to find the size in database units for text of a given point size in this EditWindow.
	 * The scale of this EditWindow is used to determine the acutal unit size.
	 * @param pointSize the size of the text in points.
	 * @return the relative size (in units) of the text.
	 */
	public double getTextUnitSize(int pointSize)
	{
		Point2D pt = deltaScreenToDatabase(pointSize, pointSize);
		return pt.getX();
	}

	/**
	 * Method to find the size in database units for text of a given point size in this EditWindow.
	 * The scale of this EditWindow is used to determine the acutal unit size.
	 * @param pointSize the size of the text in points.
	 * @return the relative size (in units) of the text.
	 */
	public int getTextPointSize(double unitSize)
	{
		Point pt = deltaDatabaseToScreen(unitSize, unitSize);
		return pt.x;
	}

	/**
	 * Method to snap a point to the nearest database-space grid unit.
	 * @param pt the point to be snapped.
	 * @param alignment the size of the snap grid (1 to round to whole numbers)
	 */
	public static void gridAlign(Point2D pt)
	{
		double alignment = User.getAlignmentToGrid();
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
	public void mousePressed(MouseEvent evt)
	{
		EditWindow wnd = (EditWindow)evt.getSource();
		WindowFrame.setCurrentWindowFrame(wnd.wf);

		curMouseListener.mousePressed(evt);
	}
	public void mouseReleased(MouseEvent evt) { curMouseListener.mouseReleased(evt); }
	public void mouseClicked(MouseEvent evt) { curMouseListener.mouseClicked(evt); }
	public void mouseEntered(MouseEvent evt)
	{
		showCoordinates(evt);
		curMouseListener.mouseEntered(evt);
	}
	public void mouseExited(MouseEvent evt) { curMouseListener.mouseExited(evt); }

	// the MouseMotionListener events
	public void mouseMoved(MouseEvent evt)
	{
		showCoordinates(evt);
		curMouseMotionListener.mouseMoved(evt);
	}
	public void mouseDragged(MouseEvent evt)
	{
		showCoordinates(evt);
		curMouseMotionListener.mouseDragged(evt);
	}

	private void showCoordinates(MouseEvent evt)
	{
		EditWindow wnd = (EditWindow)evt.getSource();
		if (wnd.getCell() == null) StatusBar.setCoordinates(null, wnd.wf); else
		{
			Point2D pt = wnd.screenToDatabase(evt.getX(), evt.getY());
			wnd.gridAlign(pt);
			StatusBar.setCoordinates("(" + pt.getX() + "," + pt.getY() + ")", wnd.wf);
		}
	}

	// the MouseWheelListener events
	public void mouseWheelMoved(MouseWheelEvent evt) { curMouseWheelListener.mouseWheelMoved(evt); }

	// the KeyListener events
	public void keyPressed(KeyEvent evt) { curKeyListener.keyPressed(evt); }
	public void keyReleased(KeyEvent evt) { curKeyListener.keyReleased(evt); }
	public void keyTyped(KeyEvent evt) { curKeyListener.keyTyped(evt); }

	// ************************************* MISCELLANEOUS *************************************

	public boolean isDoingAreaDrag() { return doingAreaDrag; }

	public void setDoingAreaDrag() { doingAreaDrag = true; }

	public void clearDoingAreaDrag() { doingAreaDrag = false; }

	public Point getStartDrag() { return startDrag; }

	public void setStartDrag(int x, int y) { startDrag.setLocation(x, y); }

	public Point getEndDrag() { return endDrag; }

	public void setEndDrag(int x, int y) { endDrag.setLocation(x, y); }
}
