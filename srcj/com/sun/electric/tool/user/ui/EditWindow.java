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

import com.sun.electric.database.change.DatabaseChangeEvent;
import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.TextUtils.WhatToSearch;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.io.output.PNG;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.Highlight2;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.MessagesStream;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.dialogs.GetInfoText;
import com.sun.electric.tool.user.dialogs.SelectObject;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.print.attribute.standard.ColorSupported;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.tree.MutableTreeNode;

/**
 * This class defines an editing window for displaying circuitry.
 * It implements WindowContent, which means it can be in the main part of a window
 * (to the right of the explorer panel).
 */
public class EditWindow extends JPanel
	implements EditWindow_, WindowContent, MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ActionListener,
	    HighlightListener, DatabaseChangeListener
{
	/** the window scale */									private double scale;
	/** the window offset */								private double offx = 0, offy = 0;
	/** the window bounds in database units */				private Rectangle2D databaseBounds;
	/** the size of the window (in pixels) */				private Dimension sz;
	/** the display cache for this window */				private Drawing drawing;
	/** the half-sizes of the window (in pixels) */			private int szHalfWidth, szHalfHeight;
	/** the cell that is in the window */					private Cell cell;
	/** the page number (for multipage schematics) */		private int pageNumber;
	/** list of edit-in-place text in this window */		private List<GetInfoText.EditInPlaceListener> inPlaceTextObjects = new ArrayList<GetInfoText.EditInPlaceListener>();

	/** true if doing down-in-place display */				private boolean inPlaceDisplay;
	/** transform from screen to cell (down-in-place only) */	private AffineTransform intoCell;
	/** transform from cell to screen (down-in-place only) */	private AffineTransform outofCell;
	/** top-level cell being displayed (down-in-place only) */	private Cell topLevelCell;
	/** path to cell being edited (down-in-place only) */	private List<NodeInst> inPlaceDescent;

	/** Cell's VarContext */                                private VarContext cellVarContext;
    /** Stack of selected PortInsts */                      private PortInst[] selectedPorts;
	/** the window frame containing this editwindow */      private WindowFrame wf;
	/** the overall panel with disp area and sliders */		private JPanel overall;

	/** the bottom scrollbar on the edit window. */			private JScrollBar bottomScrollBar;
	/** the right scrollbar on the edit window. */			private JScrollBar rightScrollBar;

	/** true if showing grid in this window */				private boolean showGrid = false;
	/** X spacing of grid dots in this window */			private double gridXSpacing;
	/** Y spacing of grid dots in this window */			private double gridYSpacing;

	/** true if doing object-selection drag */				private boolean doingAreaDrag = false;
	/** starting screen point for drags in this window */	private Point startDrag = new Point();
	/** ending screen point for drags in this window */		private Point endDrag = new Point();

	/** true if drawing popup cloud */                      private boolean showPopupCloud = false;
	/** Strings to write to popup cloud */                  private List<String> popupCloudText;
	/** lower left corner of popup cloud */                 private Point2D popupCloudPoint;

    /** Highlighter for this window */                      private Highlighter highlighter;
    /** Mouse-over Highlighter for this window */           private Highlighter mouseOverHighlighter;
    /** Ruler Highlighter for this window */                private Highlighter rulerHighlighter;

    /** navigate through saved views */                     private EditWindowFocusBrowser viewBrowser;

	/** list of windows to redraw (gets synchronized) */	private static List<EditWindow> redrawThese = new ArrayList<EditWindow>();
	/** list of window changes (synchronized) */			private static List<WindowChangeRequest> windowChangeRequests = new ArrayList<WindowChangeRequest>();
	/** true if rendering a window now (synchronized) */	private static EditWindow runningNow = null;
    /** Logger of this package. */                          private static Logger logger = Logger.getLogger("com.sun.electric.tool.user.ui");
    /** Class name for logging. */                          private static String CLASS_NAME = EditWindow.class.getName();

	private static final int SCROLLBARRESOLUTION = 200;

    /** for drawing selection boxes */	private static final BasicStroke selectionLine = new BasicStroke(
        	1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {2}, 3);
    /** for outlining down-hierarchy in-place bounds */	private static final BasicStroke inPlaceMarker = new BasicStroke(3);

	private static EditWindowDropTarget editWindowDropTarget = new EditWindowDropTarget();

	private static class WindowChangeRequest
	{
		private static final int ZOOMREQUEST   = 1;
		private static final int PANREQUEST    = 2;
		private static final int RESIZEREQUEST = 3;
		EditWindow wnd;
		double offx, offy;
		Dimension sz;
		double scale;
		int requestType;
	}

    abstract static class Drawing {
        final EditWindow wnd;

        Drawing(EditWindow wnd) {
            this.wnd = wnd;
        }

        abstract void setScreenSize(Dimension sz);

        abstract boolean paintComponent(Graphics2D g, Dimension sz);

        abstract void render(boolean fullInstantiate, Rectangle2D bounds);

        void abortRendering() {}
        
        void opacityChanged() {}
    }

    static class LayerColor {
        public final Layer layer;
        // nextRgb = inverseAlpha*prevRgb + premultipliedRgb
        public final float premultipliedRed;
        public final float premultipliedGreen;
        public final float premultipliedBlue;
        public final float inverseAlpha;

        LayerColor(Layer layer, float premultipliedRed, float premultipliedGreen, float premultipliedBlue, float inverseAlpha) {
            this.layer = layer;
            this.premultipliedRed = premultipliedRed;
            this.premultipliedGreen = premultipliedGreen;
            this.premultipliedBlue = premultipliedBlue;
            this.inverseAlpha = inverseAlpha;
        }
    }

	// ************************************* CONSTRUCTION *************************************

    // constructor
    private EditWindow(Cell cell, WindowFrame wf, Dimension approxSZ)
	{
        this.cell = cell;
        this.pageNumber = 0;
        this.wf = wf;
        setDrawingAlgorithm();
		this.gridXSpacing = User.getDefGridXSpacing();
		this.gridYSpacing = User.getDefGridYSpacing();
		inPlaceDisplay = false;
        viewBrowser = new EditWindowFocusBrowser(this);

		sz = approxSZ;
		if (sz == null) sz = new Dimension(500, 500);
		szHalfWidth = sz.width / 2;
		szHalfHeight = sz.height / 2;
		setSize(sz.width, sz.height);
		setPreferredSize(sz);
		databaseBounds = new Rectangle2D.Double();

        scale = 1;

		// the total panel in the edit window
		overall = new JPanel();
		overall.setLayout(new GridBagLayout());

		// the horizontal scroll bar
		int thumbSize = SCROLLBARRESOLUTION / 20;
		bottomScrollBar = new JScrollBar(JScrollBar.HORIZONTAL, SCROLLBARRESOLUTION/2, thumbSize, 0, SCROLLBARRESOLUTION+thumbSize);
		bottomScrollBar.setBlockIncrement(SCROLLBARRESOLUTION / 5);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		overall.add(bottomScrollBar, gbc);
		bottomScrollBar.addAdjustmentListener(new ScrollAdjustmentListener(this));
		bottomScrollBar.setValue(bottomScrollBar.getMaximum()/2);

		// the vertical scroll bar in the edit window
		rightScrollBar = new JScrollBar(JScrollBar.VERTICAL, SCROLLBARRESOLUTION/2, thumbSize, 0, SCROLLBARRESOLUTION+thumbSize);
		rightScrollBar.setBlockIncrement(SCROLLBARRESOLUTION / 5);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;   gbc.gridy = 0;
		gbc.fill = GridBagConstraints.VERTICAL;
		overall.add(rightScrollBar, gbc);
		rightScrollBar.addAdjustmentListener(new ScrollAdjustmentListener(this));
		rightScrollBar.setValue(rightScrollBar.getMaximum()/2);

		// put this object's display up
		gbc = new GridBagConstraints();
		gbc.gridx = 0;   gbc.gridy = 0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = gbc.weighty = 1;
		overall.add(this, gbc);
		setOpaque(true);
		setLayout(null);

		// a drop target for the signal panel
		DropTarget dropTargetRight = new DropTarget(this, DnDConstants.ACTION_LINK, editWindowDropTarget, true);

		//setAutoscrolls(true);

		installHighlighters();

		if (wf != null)
		{
			// make a highlighter for this window
			UserInterfaceMain.addDatabaseChangeListener(this);
			Highlighter.addHighlightListener(this);
			setCell(cell, VarContext.globalContext, null);
		}
	}

    private void setDrawingAlgorithm() {
        boolean isLayerDrawing = User.getDisplayAlgorithm() == 2 && cell != null && cell.getTechnology().isLayout();
        if (isLayerDrawing && !(drawing instanceof LayerDrawing.Drawing))
            drawing = new LayerDrawing.Drawing(this);
        else if (!isLayerDrawing && !(drawing instanceof PixelDrawing.Drawing))
            drawing = new PixelDrawing.Drawing(this);
        LayerTab layerTab = getWindowFrame().getLayersTab();
        if (layerTab != null)
            layerTab.setDisplayAlgorithm(isLayerDrawing);
    }

    private void installHighlighters()
    {
		// see if this cell is displayed elsewhere
		highlighter = mouseOverHighlighter = rulerHighlighter = null;
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame oWf = it.next();
			if (oWf.getContent() instanceof EditWindow)
			{
				EditWindow oWnd = (EditWindow)oWf.getContent();
				if (oWnd == this) continue;
				if (oWnd.getCell() == cell)
				{
					highlighter = oWnd.highlighter;
					mouseOverHighlighter = oWnd.mouseOverHighlighter;
					rulerHighlighter = oWnd.rulerHighlighter;
					break;
				}
			}
		}
		if (highlighter == null)
		{
			// not shown elsewhere: create highlighters
			highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, wf);
	        mouseOverHighlighter = new Highlighter(Highlighter.MOUSEOVER_HIGHLIGHTER, wf);
			rulerHighlighter = new Highlighter(Highlighter.RULER_HIGHLIGHTER, wf);
		}

        // add listeners --> BE SURE to remove listeners in finished()
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
   }

    private void uninstallHighlighters()
    {
		removeKeyListener(this);
		removeMouseListener(this);
		removeMouseMotionListener(this);
		removeMouseWheelListener(this);

        // see if the highlighters are used elsewhere
        boolean used = false;
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame oWf = it.next();
			if (oWf.getContent() instanceof EditWindow)
			{
				EditWindow oWnd = (EditWindow)oWf.getContent();
				if (oWnd == this) continue;
				if (oWnd.getCell() == cell)
				{
					used = true;
					break;
				}
			}
		}

		if (!used)
		{
	        highlighter.delete();
	        mouseOverHighlighter.delete();
			rulerHighlighter.delete();
		}
    }

    /**
	 * Factory method to create a new EditWindow with a given cell, in a given WindowFrame.
	 * @param cell the cell in this EditWindow.
	 * @param wf the WindowFrame that this EditWindow lives in.
	 * @param approxSZ the approximate size of this EditWindow (in pixels).
	 * @return the new EditWindow.
	 */
	public static EditWindow CreateElectricDoc(Cell cell, WindowFrame wf, Dimension approxSZ)
	{
		EditWindow ui = new EditWindow(cell, wf, approxSZ);
		return ui;
	}

	// ************************************* EVENT LISTENERS *************************************

	private int lastXPosition, lastYPosition;

	/**
	 * Respond to an action performed, in this case change the current cell
	 * when the user clicks on an entry in the upHierarchy popup menu.
	 */
	public void actionPerformed(ActionEvent e)
	{
		JMenuItem source = (JMenuItem)e.getSource();
		// extract library and cell from string
		Cell cell = (Cell)Cell.findNodeProto(source.getText());
		if (cell == null) return;
        Cell currentCell = getCell();
		setCell(cell, VarContext.globalContext, null);

		// Highlight an instance of cell we came from in current cell
        highlighter.clear();
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            if (ni.isCellInstance()) {
                Cell nodeCell = (Cell)ni.getProto();
                if (nodeCell == currentCell) {
                    highlighter.addElectricObject(ni, cell);
                    break;
                }
                if (nodeCell.isIconOf(currentCell)) {
                    highlighter.addElectricObject(ni, cell);
                    break;
                }
            }
        }
        highlighter.finished();
	}

	// the MouseListener events
	public void mousePressed(MouseEvent evt)
	{
        requestFocus();
		MessagesStream.userCommandIssued();
		lastXPosition = evt.getX();   lastYPosition = evt.getY();
		WindowFrame.curMouseListener.mousePressed(evt);
	}

	public void mouseReleased(MouseEvent evt)
	{
		lastXPosition = evt.getX();   lastYPosition = evt.getY();
		WindowFrame.curMouseListener.mouseReleased(evt);
	}

	public void mouseClicked(MouseEvent evt)
	{
		lastXPosition = evt.getX();   lastYPosition = evt.getY();
		WindowFrame.curMouseListener.mouseClicked(evt);
	}

	public void mouseEntered(MouseEvent evt)
	{
		lastXPosition = evt.getX();   lastYPosition = evt.getY();
		showCoordinates(evt);
		WindowFrame.curMouseListener.mouseEntered(evt);
	}

	public void mouseExited(MouseEvent evt)
	{
		lastXPosition = evt.getX();   lastYPosition = evt.getY();
		WindowFrame.curMouseListener.mouseExited(evt);
	}

	// the MouseMotionListener events
	public void mouseMoved(MouseEvent evt)
	{
		lastXPosition = evt.getX();   lastYPosition = evt.getY();
		showCoordinates(evt);
		WindowFrame.curMouseMotionListener.mouseMoved(evt);
	}

	public void mouseDragged(MouseEvent evt)
	{
		lastXPosition = evt.getX();   lastYPosition = evt.getY();
		showCoordinates(evt);
		WindowFrame.curMouseMotionListener.mouseDragged(evt);
	}

	private void showCoordinates(MouseEvent evt)
	{
		EditWindow wnd = (EditWindow)evt.getSource();
		if (wnd.getCell() == null) StatusBar.setCoordinates(null, wnd.wf); else
		{
			Point2D pt = wnd.screenToDatabase(evt.getX(), evt.getY());
			EditWindow.gridAlign(pt);

			if (User.isShowHierarchicalCursorCoordinates())
			{
				// if the current VarContext is not the global one, user is "down hierarchy"
				String path = null;
				if (cellVarContext != VarContext.globalContext)
				{
					Point2D ptPath = new Point2D.Double(pt.getX(), pt.getY());
					VarContext vc = cellVarContext;
					boolean validPath = true;
					boolean first = true;
					NodeInst ni = null;
					path = "";
					while (vc != VarContext.globalContext)
					{
						Nodable no = vc.getNodable();
						if (!(no instanceof NodeInst))
						{
							validPath = false;
							break;
						}
						ni = (NodeInst)no;
//						if (((Cell)ni.getProto()).getView() == View.ICON)
//						{
//							validPath = false;
//							break;
//						}
						//path = ni.describe() + path;
                        path = ni.getParent().getName() + "[" + ni.getName() + "]" + (first? "" : " / ") + path;
                        if (first) first = false;
						AffineTransform trans = ni.translateOut(ni.rotateOut());
						trans.transform(ptPath, ptPath);
						vc = vc.pop();
					}
					if (validPath)
					{
                        if (ni.getParent().getView() == View.SCHEMATIC) {
                            path = "Location is " + ni.getParent() + " / " + path;
                        } else {
                            path = "Location in " + ni.getParent() + " / " + path + " is (" +
                                TextUtils.formatDouble(ptPath.getX(), 2) + ", " + TextUtils.formatDouble(ptPath.getY(), 2) + ")";
                        }
					} else path = null;
				}
				StatusBar.setHierarchicalCoordinates(path, wnd.wf);
			}

			StatusBar.setCoordinates("(" + TextUtils.formatDouble(pt.getX(), 2) + ", " + TextUtils.formatDouble(pt.getY(), 2) + ")", wnd.wf);
		}
	}

	// the MouseWheelListener events
	public void mouseWheelMoved(MouseWheelEvent evt) { WindowFrame.curMouseWheelListener.mouseWheelMoved(evt); }

	// the KeyListener events
	public void keyPressed(KeyEvent evt)
	{
		MessagesStream.userCommandIssued();
		WindowFrame.curKeyListener.keyPressed(evt);
	}

	public void keyReleased(KeyEvent evt) { WindowFrame.curKeyListener.keyReleased(evt); }

	public void keyTyped(KeyEvent evt) { WindowFrame.curKeyListener.keyTyped(evt); }

    public void highlightChanged(Highlighter which) {
		repaint();
    }

    /**
     * Called when by a Highlighter when it loses focus. The argument
     * is the Highlighter that has gained focus (may be null).
     * @param highlighterGainedFocus the highlighter for the current window (may be null).
     */
    public void highlighterLostFocus(Highlighter highlighterGainedFocus) {}

	public Point getLastMousePosition()
	{
		return new Point(lastXPosition, lastYPosition);
	}

	// ************************************* WHEN DROPPING A CELL NAME FROM THE EXPLORER TREE *************************************

	/**
	 * Method to set the highlight to show the outline of a node that will be placed in this EditWindow.
	 * @param toDraw the object to draw (a NodeInst or a NodeProto).
	 * @param oldx the X position (on the screen) of the outline.
	 * @param oldy the Y position (on the screen) of the outline.
	 */
	public void showDraggedBox(Object toDraw, int oldx, int oldy)
	{
		// undraw it
        Highlighter highlighter = getHighlighter();
		highlighter.clear();

		// draw it
		Point2D drawnLoc = screenToDatabase(oldx, oldy);
		EditWindow.gridAlign(drawnLoc);
		NodeProto np = null;
		if (toDraw instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)toDraw;
			np = ni.getProto();
		}
		if (toDraw instanceof NodeProto)
		{
			np = (NodeProto)toDraw;
		}
		int defAngle = 0;
		if (toDraw instanceof NodeInst)
		{
			NodeInst ni = (NodeInst)toDraw;
			defAngle = ni.getAngle();
		}
		if (toDraw instanceof PrimitiveNode)
		{
			defAngle = ((PrimitiveNode)toDraw).getDefPlacementAngle();
		}
		if (np != null)
		{
			// zoom the window to fit the placed node (if appropriate)
			zoomWindowToFitCellInstance(np);

			Poly poly = null;
            Orientation orient = Orientation.fromJava(defAngle, defAngle >= 3600, false);
			if (np instanceof Cell)
			{
				Cell placeCell = (Cell)np;
				Rectangle2D cellBounds = placeCell.getBounds();
				poly = new Poly(cellBounds);

				AffineTransform rotate = orient.pureRotate();
				AffineTransform translate = new AffineTransform();
				translate.setToTranslation(drawnLoc.getX(), drawnLoc.getY());
				rotate.concatenate(translate);
				poly.transform(rotate);
			} else
			{
				SizeOffset so = np.getProtoSizeOffset();
				double trueSizeX = np.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
				double trueSizeY = np.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
				double dX = (so.getHighXOffset() - so.getLowXOffset())/2;
				double dY = (so.getHighYOffset() - so.getLowYOffset())/2;
				poly = new Poly(drawnLoc.getX()-dX, drawnLoc.getY()-dY, trueSizeX, trueSizeY);
				AffineTransform trans = orient.rotateAbout(drawnLoc.getX(), drawnLoc.getY());
				poly.transform(trans);
			}
			Point2D [] points = poly.getPoints();
			for(int i=0; i<points.length; i++)
			{
				int last = i-1;
				if (i == 0) last = points.length - 1;
				highlighter.addLine(points[last], points[i], getCell());
			}
			repaint();
		}
		highlighter.finished();
	}

	/**
	 * Method to zoom this window to fit a placed node (if appropriate).
	 * If the placed object is a cell instance that is larger than the window,
	 * and the window is empty, zoom out to fit.
	 * @param np the node being placed.
	 */
	public void zoomWindowToFitCellInstance(NodeProto np)
	{
		Cell parent = getCell();
		if (parent == null) return;
		boolean empty = true;
		if (parent.getNumArcs() > 0) empty = false;
		if (parent.getNumNodes() > 1) empty = false; else
		{
			if (parent.getNumNodes() == 1)
			{
				NodeInst onlyNi = parent.getNode(0);
				if (onlyNi.getProto() != Generic.tech.cellCenterNode) empty = false;
			}
		}
		if (empty && np instanceof Cell)
		{
			// placing instance of cell into empty cell: see if scaling is necessary
			Rectangle2D cellBounds = ((Cell)np).getBounds();
			Rectangle2D screenBounds = displayableBounds();
			if (cellBounds.getWidth() > screenBounds.getWidth() ||
				cellBounds.getHeight() > screenBounds.getHeight())
			{
				double scaleX = cellBounds.getWidth() / (screenBounds.getWidth() * 0.9);
				double scaleY = cellBounds.getHeight() / (screenBounds.getHeight() * 0.9);
				double scale = Math.max(scaleX, scaleY);
				setScale(getScale() / scale);
			}
		}
	}

	/**
	 * Class to define a custom data flavor that packages a NodeProto to create.
	 */
	public static class NodeProtoDataFlavor extends DataFlavor
	{
		private Cell cell;
		private Cell.CellGroup group;
		private ExplorerTree originalTree;

		NodeProtoDataFlavor(Cell cell, Cell.CellGroup group, ExplorerTree originalTree)
			throws ClassNotFoundException
		{
			super(NodeProto.class, "electric/instance");
			this.cell = cell;
			this.group = group;
			this.originalTree = originalTree;
		}

		public Object getFlavorObject()
		{
			if (cell != null) return cell;
			return group;
		}

		public ExplorerTree getOriginalTree() { return originalTree; }
	}

	/**
	 * Class to define a custom transferable that packages a Cell or Group.
	 */
	public static class NodeProtoTransferable implements Transferable
	{
		private Cell cell;
		private Cell.CellGroup group;
		private NodeProtoDataFlavor df;

		public NodeProtoTransferable(Object obj, ExplorerTree tree)
		{
			if (obj instanceof Cell)
			{
				cell = (Cell)obj;
				group = cell.getCellGroup();
			} else if (obj instanceof Cell.CellGroup)
			{
				group = (Cell.CellGroup)obj;
			}
			try
			{
				df = new NodeProtoDataFlavor(cell, group, tree);
			} catch (ClassNotFoundException e)
			{
				System.out.println("ERROR: Cannot make Electric DataFlavor");
			}
		}

		public boolean isValid() { return group != null; }

		public DataFlavor[] getTransferDataFlavors()
		{
			DataFlavor [] it = new DataFlavor[1];
			it[0] = df;
			return it;
		}

		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			if (flavor == df) return true;
			return false;
		}

		public Object getTransferData(DataFlavor flavor)
		{
			if (flavor != df) return null;
			if (cell != null) return cell;
			return group;
		}
	}

	/**
	 * Class for catching drags into the edit window.
	 * These drags come from the Explorer tree (when a cell name is dragged to place an instance).
	 */
	private static class EditWindowDropTarget implements DropTargetListener
	{
		public void dragEnter(DropTargetDragEvent e)
		{
			dragAction(e);
		}

		public void dragOver(DropTargetDragEvent e)
		{
			dragAction(e);
		}

		private Object getDraggedObject(DataFlavor [] flavors)
		{
			if (flavors.length > 0)
			{
				if (flavors[0] instanceof NodeProtoDataFlavor)
				{
					NodeProtoDataFlavor npdf = (NodeProtoDataFlavor)flavors[0];
					Object obj = npdf.getFlavorObject();
					return obj;
				}
			}
			return null;
		}

		private void dragAction(DropTargetDragEvent e)
		{
			Object obj = getDraggedObject(e.getCurrentDataFlavors());
			if (obj != null)
			{
				e.acceptDrag(e.getDropAction());

				// determine the window
				DropTarget dt = (DropTarget)e.getSource();
				if (dt.getComponent() instanceof JPanel)
				{
					EditWindow wnd = (EditWindow)dt.getComponent();
					wnd.showDraggedBox(obj, e.getLocation().x, e.getLocation().y);
				}
				return;
			}
		}

		public void dropActionChanged(DropTargetDragEvent e)
		{
			e.acceptDrag(e.getDropAction());
		}

		public void dragExit(DropTargetEvent e) {}

		public void drop(DropTargetDropEvent dtde)
		{
			dtde.acceptDrop(DnDConstants.ACTION_LINK);
			Object obj = getDraggedObject(dtde.getCurrentDataFlavors());
			if (obj == null)
			{
				dtde.dropComplete(false);
				return;
			}

			// determine the window
			DropTarget dt = (DropTarget)dtde.getSource();
			if (!(dt.getComponent() instanceof JPanel))
			{
				dtde.dropComplete(false);
				return;
			}
			EditWindow wnd = (EditWindow)dt.getComponent();
			Point2D where = wnd.screenToDatabase(dtde.getLocation().x, dtde.getLocation().y);
			EditWindow.gridAlign(where);
            wnd.getHighlighter().clear();

			NodeInst ni = null;
			NodeProto np = null;
			int defAngle = 0;
			if (obj instanceof NodeProto)
			{
				np = (NodeProto)obj;
				if (np instanceof PrimitiveNode)
					defAngle = ((PrimitiveNode)np).getDefPlacementAngle();
			} else if (obj instanceof NodeInst)
			{
				ni = (NodeInst)obj;
				np = ni.getProto();
			}
			new PaletteFrame.PlaceNewNode("Create Node", np, ni, defAngle, where, wnd.getCell(), null, false);
		}
	}

	// ************************************* INFORMATION *************************************

	/**
	 * Method to return the top-level JPanel for this EditWindow.
	 * The actual EditWindow object is below the top level, surrounded by scroll bars.
	 * @return the top-level JPanel for this EditWindow.
	 */
	public JPanel getPanel() { return overall; }

	/**
	 * Method to return the location of this window on the user's screens.
	 * @return the location of this window on the user's screens.
	 */
	public Point getScreenLocationOfCorner() { return overall.getLocationOnScreen(); }

	/**
	 * Method to return the current EditWindow.
	 * @return the current EditWindow (null if none).
	 */
	public static EditWindow getCurrent()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
		if (wf == null) return null;
		if (wf.getContent() instanceof EditWindow) return (EditWindow)wf.getContent();
		return null;
	}

	/**
	 * Method to return the current EditWindow.
	 * @return the current EditWindow.
	 * If there is none, an error message is displayed and it returns null.
	 */
	public static EditWindow needCurrent()
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
		if (wf != null)
		{
			if (wf.getContent() instanceof EditWindow) return (EditWindow)wf.getContent();
		}
		System.out.println("There is no current window for this operation");
		return null;
	}

	// ************************************* EDITWINDOW METHODS *************************************

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return cell; }


	/**
	 * Method to set the page number that is shown in this window.
	 * Only applies to multi-page schematics.
	 * @param pageNumber the page number that is shown in this window (0-based).
	 */
	public void setMultiPageNumber(int pageNumber)
	{
		if (this.pageNumber == pageNumber) return;
		this.pageNumber = pageNumber;
		setWindowTitle();
		fillScreen();
	}

	/**
	 * Method to return the page number that is shown in this window.
	 * Only applies to multi-page schematics.
	 * @return the page number that is shown in this window (0-based).
	 */
	public int getMultiPageNumber() { return pageNumber; }

	/**
	 * Method to tell whether this EditWindow is displaying a cell "in-place".
	 * In-place display implies that the user has descended into a lower-level
	 * cell while requesting that the upper-level remain displayed.
	 * @return true if this EditWindow is displaying a cell "in-place".
	 */
	public boolean isInPlaceEdit() { return inPlaceDisplay; }

	/**
	 * Method to return the top-level cell for "in-place" display.
	 * In-place display implies that the user has descended into a lower-level
	 * cell while requesting that the upper-level remain displayed.
	 * The top-level cell is the original cell that is remaining displayed.
	 * @return the top-level cell for "in-place" display.
	 */
	public Cell getInPlaceEditTopCell() { return topLevelCell; }

	/**
	 * Method to return a List of NodeInsts to the cell being in-place edited.
	 * In-place display implies that the user has descended into a lower-level
	 * cell while requesting that the upper-level remain displayed.
	 * @return a List of NodeInsts to the cell being in-place edited.
	 */
	public List<NodeInst> getInPlaceEditNodePath() { return inPlaceDescent; }

	/**
	 * Method to set the List of NodeInsts to the cell being in-place edited.
	 * In-place display implies that the user has descended into a lower-level
	 * cell while requesting that the upper-level remain displayed.
	 * @param list the List of NodeInsts to the cell being in-place edited.
	 */
	public void setInPlaceEditNodePath(List<NodeInst> list) { inPlaceDescent = list; }

	/**
	 * Method to return the transformation matrix from the displayed top-level cell to the current cell.
	 * In-place display implies that the user has descended into a lower-level
	 * cell while requesting that the upper-level remain displayed.
	 * @return the transformation matrix from the displayed top-level cell to the current cell.
	 */
	public AffineTransform getInPlaceTransformIn() { return intoCell; }

	/**
	 * Method to return the transformation matrix from the current cell to the displayed top-level cell.
	 * In-place display implies that the user has descended into a lower-level
	 * cell while requesting that the upper-level remain displayed.
	 * @return the transformation matrix from the current cell to the displayed top-level cell.
	 */
	public AffineTransform getInPlaceTransformOut() { return outofCell; }

	/**
     * Get the highlighter for this WindowContent
     * @return the highlighter
     */
    public Highlighter getHighlighter() { return highlighter; }

    /**
     * Get the mouse over highlighter for this EditWindow
     * @return the mouse over highlighter
     */
    public Highlighter getMouseOverHighlighter() { return mouseOverHighlighter; }

    /**
     * Get the ruler highlighter for this EditWindow (for measurement)
     * @return the ruler highlighter
     */
    public Highlighter getRulerHighlighter() { return rulerHighlighter; }

	/**
	 * Method to return the WindowFrame in which this EditWindow resides.
	 * @return the WindowFrame in which this EditWindow resides.
	 */
	public WindowFrame getWindowFrame() { return wf; }

	/**
	 * Method to set the cell that is shown in the window to "cell".
	 */
	public void setCell(Cell cell, VarContext context, WindowFrame.DisplayAttributes displayAttributes)
	{
		// by default record history and fillscreen
		// However, when navigating through history, don't want to record new history objects.
        if (context == null) context = VarContext.globalContext;
        boolean fillTheScreen = false;
		if (displayAttributes == null)
		{
			displayAttributes = new WindowFrame.DisplayAttributes();
			displayAttributes.scale = scale;
			displayAttributes.offX = offx;
			displayAttributes.offY = offy;
			displayAttributes.inPlace = false;
			fillTheScreen = true;
		}
		showCell(cell, context, fillTheScreen, displayAttributes);
	}

	/**
	 * Method to show a cell with ports, display factors, etc.
	 */
	private void showCell(Cell cell, VarContext context, boolean fillTheScreen,
		WindowFrame.DisplayAttributes displayAttributes)
	{
		// record current history before switching to new cell
		wf.saveCurrentCellHistoryState();

		// remove highlighters from the window
		uninstallHighlighters();

		// set new values
		this.cell = cell;
		inPlaceDisplay = displayAttributes.inPlace;
		intoCell = displayAttributes.intoCell;
		outofCell = displayAttributes.outofCell;
		topLevelCell = displayAttributes.topLevelCell;
		inPlaceDescent = displayAttributes.inPlaceDescent;
		this.pageNumber = 0;
		cellVarContext = context;
        if (cell != null) {
            Library lib = cell.getLibrary();
            Job.getUserInterface().setCurrentCell(lib, cell);
        }
        setDrawingAlgorithm();

        // add new highlighters from the window
		installHighlighters();
        viewBrowser.clear();

		setWindowTitle();
		if (wf != null)
		{
			if (cell != null)
			{
				if (wf == WindowFrame.getCurrentWindowFrame(false))
				{
					// if auto-switching technology, do it
					WindowFrame.autoTechnologySwitch(cell, wf);
				}
			}
		}
		if (fillTheScreen) fillScreen(); else
		{
			setScale(displayAttributes.scale);
			setOffset(new Point2D.Double(displayAttributes.offX, displayAttributes.offY));
		}

		if (cell != null && User.isCheckCellDates()) cell.checkCellDates();

		// clear list of cross-probed levels for this EditWindow
		clearCrossProbeLevels();
	}

	/**
	 * Method to set the window title.
	 */
	public void setWindowTitle()
	{
		if (wf == null) return;
		wf.setTitle(wf.composeTitle(cell, "", pageNumber));
	}

	/**
	 * Method to find an EditWindow that is displaying a given cell.
	 * @param cell the Cell to find.
	 * @return the EditWindow showing that cell, or null if none found.
	 */
	public static EditWindow findWindow(Cell cell)
	{
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			if (content.getCell() == cell) return (EditWindow)content;
		}
		return null;
	}

	public List<MutableTreeNode> loadExplorerTrees()
	{
        return wf.loadDefaultExplorerTree();
	}

	/**
	 * Method to get rid of this EditWindow.  Called by WindowFrame when
	 * that windowFrame gets closed.
	 */
	public void finished()
	{
		//wf = null;                          // clear reference
		//offscreen = null;                   // need to clear this ref, because it points to this

		// remove myself from listener list
		uninstallHighlighters();
        UserInterfaceMain.removeDatabaseChangeListener(this);
		Highlighter.removeHighlightListener(this);
	}

	// ************************************* SCROLLING *************************************

	/**
	 * Method to return the scroll bar resolution.
	 * This is the extent of the JScrollBar.
	 * @return the scroll bar resolution.
	 */
	public static int getScrollBarResolution() { return SCROLLBARRESOLUTION; }

	/**
	 * This class handles changes to the edit window scroll bars.
	 */
	private static class ScrollAdjustmentListener implements AdjustmentListener
	{
        /** A weak reference to the WindowFrame */
		EditWindow wnd;

		ScrollAdjustmentListener(EditWindow wnd)
		{
			super();
			this.wnd = wnd;
//			this.wf = new WeakReference(wf);
		}

		public void adjustmentValueChanged(AdjustmentEvent e)
		{
			if (e.getSource() == wnd.getBottomScrollBar() && wnd.getCell() != null)
				wnd.bottomScrollChanged(e.getValue());
			if (e.getSource() == wnd.getRightScrollBar() && wnd.getCell() != null)
				wnd.rightScrollChanged(e.getValue());
		}
	}

	/**
	 * Method to return the horizontal scroll bar at the bottom of the edit window.
	 * @return the horizontal scroll bar at the bottom of the edit window.
	 */
	public JScrollBar getBottomScrollBar() { return bottomScrollBar; }

	/**
	 * Method to return the vertical scroll bar at the right side of the edit window.
	 * @return the vertical scroll bar at the right side of the edit window.
	 */
	public JScrollBar getRightScrollBar() { return rightScrollBar; }

	// ************************************* REPAINT *************************************

//	private boolean intermediateRefresh = false;
//
//	public void allowIntermediateRefresh()
//	{
//		intermediateRefresh = true;
//	}

	/**
	 * Method to repaint this EditWindow.
	 * Composites the image (taken from the PixelDrawing object)
	 * with the grid, highlight, and any dragging rectangle.
	 */
	public void paintComponent(Graphics graphics)
	{
		// to enable keys to be received
		if (wf == null) return;
//		if (wf == WindowFrame.getCurrentWindowFrame())
//			requestFocusInWindow();

        Graphics2D g = (Graphics2D)graphics;
        if (cell == null) {
            g.setColor(new Color(User.getColorBackground()));
            Dimension sz = getSize();
            g.fillRect(0, 0, sz.width, sz.height);
            String msg = "No cell in this window";
            Font f = new Font(User.getDefaultFont(), Font.BOLD, 18);
            g.setFont(f);
            g.setColor(new Color(User.getColorText()));
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.drawString("No cell in this window", (sz.width - g.getFontMetrics(f).stringWidth(msg))/2, sz.height/2);
            return;
        }

        logger.entering(CLASS_NAME, "paintComponent", this);

        if (!drawing.paintComponent(g, sz)) {
            Dimension newSize = getSize();
            setScreenSize(newSize);
            repaintContents(null, false);
            g.setColor(new Color(User.getColorBackground()));
            g.fillRect(0, 0, newSize.width, newSize.height);
            logger.exiting(CLASS_NAME, "paintComponent", "resize and repaint");
            return;
        }
        logger.logp(Level.FINER, CLASS_NAME, "paintComponent", "offscreen is drawn");

		// add in grid if requested
//		if (showGrid) drawGrid(g);

		// add cross-probed level display
		showCrossProbeLevels(g);

        if (Job.getDebug()) {
            // add in highlighting
	        if (Job.acquireExamineLock(false)) {
	            try {
	                // add in the frame if present
	                drawCellFrame(g);

	                //long start = System.currentTimeMillis();
	                rulerHighlighter.showHighlights(this, g);
	                mouseOverHighlighter.showHighlights(this, g);
	                highlighter.showHighlights(this, g);
	                //long end = System.currentTimeMillis();
	                //System.out.println("drawing highlights took "+TextUtils.getElapsedTime(end-start));
	                Job.releaseExamineLock();
	            } catch (Error e) {
	                Job.releaseExamineLock();
	                throw e;
	            }
	        } else {
	          	// repaint
/*
	            TimerTask redrawTask = new TimerTask() {
	                public void run() { repaint(); }
	            };
	            Timer timer = new Timer();
	            timer.schedule(redrawTask, 1000);
*/
	        }
        } else {
            // unsafe
            try {
                // add in the frame if present
                drawCellFrame(g);

                //long start = System.currentTimeMillis();
                rulerHighlighter.showHighlights(this, g);
                mouseOverHighlighter.showHighlights(this, g);
                highlighter.showHighlights(this, g);
                //long end = System.currentTimeMillis();
                //System.out.println("drawing highlights took "+TextUtils.getElapsedTime(end-start));
            } catch (Exception e) {
            }
        }

		// add in drag area
		if (doingAreaDrag) showDragBox(g);

		// add in popup cloud
		if (showPopupCloud) drawPopupCloud((Graphics2D)g);

		// add in shadow if doing in-place editing
		if (inPlaceDisplay)
		{
			Rectangle2D bounds = cell.getBounds();
			Point i1 = databaseToScreen(bounds.getMinX(), bounds.getMinY());
			Point i2 = databaseToScreen(bounds.getMinX(), bounds.getMaxY());
			Point i3 = databaseToScreen(bounds.getMaxX(), bounds.getMaxY());
			Point i4 = databaseToScreen(bounds.getMaxX(), bounds.getMinY());

			// shade everything else except for the cell being edited
			Polygon innerPoly = new Polygon();
			innerPoly.addPoint(i1.x, i1.y);
			innerPoly.addPoint(i2.x, i2.y);
			innerPoly.addPoint(i3.x, i3.y);
			innerPoly.addPoint(i4.x, i4.y);
			Area outerArea = new Area(new Rectangle(0, 0, sz.width, sz.height));
			Area innerArea = new Area(innerPoly);
			outerArea.subtract(innerArea);
			g.setColor(new Color(128, 128, 128, 128));
			g.fill(outerArea);

			// draw a red box around the cell being edited
			g.setStroke(inPlaceMarker);
			g.setColor(Color.RED);
			g.drawLine(i1.x, i1.y, i2.x, i2.y);
			g.drawLine(i2.x, i2.y, i3.x, i3.y);
			g.drawLine(i3.x, i3.y, i4.x, i4.y);
			g.drawLine(i4.x, i4.y, i1.x, i1.y);
		}
        logger.logp(Level.FINER, CLASS_NAME, "paintComponent", "overlays are drawn");

		// draw any components that are on top (such as in-line text edits)
//		for(GetInfoText.EditInPlaceListener tl : inPlaceTextObjects)
//		{
//			tl.getTextComponent().paint(g);
//		}
//		super.paint(g);

		// see if anything else is queued
		synchronized(redrawThese)
		{
            logger.logp(Level.FINEST, CLASS_NAME, "paintComponent", "checkForOtherRequests");
			if (runningNow == null)
			{
				if (handleWindowChangeRequests(this))
				{
					if (!redrawThese.contains(this))
						redrawThese.add(this);
				}
				if (redrawThese.size() > 0)
				{
					runningNow = redrawThese.get(0);
					redrawThese.remove(0);
                    logger.logp(Level.FINER, CLASS_NAME, "paintComponent", "restart RenderJob");
					RenderJob nextJob = new RenderJob(runningNow, null, false);
                    logger.exiting(CLASS_NAME, "paintComponent");
					return;
				}
			}
		}
        logger.exiting(CLASS_NAME, "paintComponent");
	}

	/**
	 * Method to store a new "in-place" text editing object on this EditWindow.
	 * @param tl the Listener that is now sitting on top of this EditWindow.
	 */
	public void addInPlaceTextObject(GetInfoText.EditInPlaceListener tl)
	{
		inPlaceTextObjects.add(tl);
		add(tl.getTextComponent());
	}

	/**
	 * Method to remove a "in-place" text editing object from this EditWindow.
	 * @param tl the Listener that is no longer sitting on top of this EditWindow.
	 */
	public void removeInPlaceTextObject(GetInfoText.EditInPlaceListener tl)
	{
		inPlaceTextObjects.remove(tl);
		remove(tl.getTextComponent());
	}

	/**
	 * Method to remove all in-place text objects in this window.
	 * Called when the window pans or zooms and the text objects are no longer in the proper place.
	 */
	public void removeAllInPlaceTextObjects()
	{
		List<GetInfoText.EditInPlaceListener> allTextObjects = new ArrayList<GetInfoText.EditInPlaceListener>();
		for(GetInfoText.EditInPlaceListener eip : inPlaceTextObjects)
			allTextObjects.add(eip);
		for(GetInfoText.EditInPlaceListener tl : allTextObjects)
		{
			tl.closeEditInPlace();
		}
	}

	public void fullRepaint() { repaintContents(null, false); }

	/**
	 * Method requests that every EditWindow be redrawn, including a change of display algorithm.
	 */
	public static void displayAlgorithmChanged()
	{
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)content;
            wnd.setDrawingAlgorithm();
			wnd.repaintContents(null, false);
		}
	}

	/**
	 * Method requests that every EditWindow be redrawn, including a rerendering of its contents.
	 */
	public static void repaintAllContents()
	{
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)content;
			wnd.repaintContents(null, false);
		}
	}

	/**
	 * Method requests that every EditWindow be redrawn, without rerendering the offscreen contents.
	 */
	public static void repaintAll()
	{
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)content;
			wnd.repaint();
		}
	}

	/**
	 * Method requests that this EditWindow be redrawn, including a rerendering of the contents.
	 * @param bounds the area to redraw (null to draw everything).
	 * @param fullInstantiate true to display to the bottom of the hierarchy (for peeking).
	 */
	public void repaintContents(Rectangle2D bounds, boolean fullInstantiate)
	{
		// start rendering thread
		if (wf == null) return;
//		if (drawing.offscreen == null) return;
        if (cell == null) {
            repaint();
            return;
        }

        logger.entering(CLASS_NAME, "repaintContents", bounds);
		// do the redraw in a separate thread
		synchronized(redrawThese)
		{
			if (runningNow != null)
			{
				if (runningNow == this)
					drawing.abortRendering();
                if (!redrawThese.contains(this))
                    redrawThese.add(this);
                logger.exiting(CLASS_NAME, "repaintContents");
				return;
			}
			handleWindowChangeRequests(this);
			runningNow = this;
		}
		RenderJob renderJob = new RenderJob(this, bounds, fullInstantiate);

        // do the redraw in the main thread
        setScrollPosition();                        // redraw scroll bars
        logger.exiting(CLASS_NAME, "repaintContents");
	}

    private final static String RENDER_JOB_CLASS_NAME = CLASS_NAME + ".RenderJob";
    /**
	 * This class queues requests to rerender a window.
	 */
	private static class RenderJob extends Job
	{
		private EditWindow wnd;
		private Rectangle2D bounds;
		private boolean fullInstantiate;

		protected RenderJob(EditWindow wnd, Rectangle2D bounds, boolean fullInstantiate)
		{
			super("Display", User.getUserTool(), Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.wnd = wnd;
			this.bounds = bounds;
			this.fullInstantiate = fullInstantiate;
			startJob();
		}

		public boolean doIt() throws JobException
		{
            logger.entering(RENDER_JOB_CLASS_NAME, "doIt");
			if (bounds == null)
			{
				// see if a real bounds is defined in the cell
				bounds = User.getChangedInWindow(wnd);
				if (bounds != null)
				{
					User.clearChangedInWindow(wnd);
//					wnd.highlighter.addArea(bounds, cell);
				}
			}

			// do the hard work of re-rendering the image
            try
            {
				wnd.drawing.render(fullInstantiate, bounds);
            } catch (java.util.ConcurrentModificationException e)
            {
				System.out.println("GOT ConcurrentModificationException during redisplay!");
            	ActivityLogger.logException(e);
				wnd.repaintContents(bounds, fullInstantiate);
            } finally {
                synchronized(redrawThese) {
                    runningNow = null;
                }
            }
			wnd.repaint();
            logger.exiting(RENDER_JOB_CLASS_NAME, "doIt");
			return true;
		}
	}

	/**
	 * Method to automatically set the opacity of each layer in a Technology.
	 * At the current time, this method is not used.
	 * @param tech the Technology to set.
	 */
	public static void setDefaultOpacity(Technology tech)
	{
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
        {
			Layer layer = it.next();
        	Layer.Function fun = layer.getFunction();
        	int extra = layer.getFunctionExtras();
        	double opacity = 0.4;
        	if (fun.isMetal())
        	{
        		opacity = 0.75 - fun.getLevel() * 0.05;
        	} else if (fun.isContact())
        	{
        		if ((extra&Layer.Function.CONMETAL) != 0) opacity = 0.7; else
        			opacity = 1;
        	} else if (fun == Layer.Function.OVERGLASS)
        	{
        		opacity = 0.2;
            } else if (fun == Layer.Function.POLY1 ||
                       fun == Layer.Function.POLY2 ||
                       fun == Layer.Function.POLY3 ||
                       fun == Layer.Function.GATE ||
                       fun == Layer.Function.DIFF ||
                       fun == Layer.Function.DIFFP ||
                       fun == Layer.Function.DIFFN ||
                       fun == Layer.Function.WELL ||
                       fun == Layer.Function.WELLP ||
                       fun == Layer.Function.WELLN ||
                       fun == Layer.Function.IMPLANT ||
                       fun == Layer.Function.IMPLANTN ||
                       fun == Layer.Function.IMPLANTP) {
                // lowest level layers should all be opaque
                opacity = 1.0;
            } else if (fun == Layer.Function.ART) {
                if (layer.getName().equals("Glyph"))
                    opacity = 0;        // essential bounds
            }
//			if (!layer.isVisible()) opacity = 0;
			layer.getGraphics().setOpacity(opacity);
        }
	}

	/**
     * Returns alpha blending order for this EditWindow.
     * Alpha blending order specifies pixel color by such a way:
     * Color col = backgroudColor;
     * for (LayerColor layerColor: blendingOrder) {
     *    if (This pixel covers a piece of layer layerColor.layer) {
     *       alpha = layerColor.color.getAlpha();
     *       col =  layerColor.color.getRGB()*alpha + col*(1 - alpha)
     *    }
     * }
     * return col;
     * @param layersAvailable layers available in this EditWindow
     * @param showOpacity show opacity controls in LayerTab.
     * @return alpha blending order.
     */
    List<LayerColor> getBlendingOrder(Set<Layer> layersAvailable, boolean patternedDisplay, boolean alphaBlendingOvercolor) {
        ArrayList<LayerColor> layerColors = new ArrayList<LayerColor>();
        ArrayList<Layer> sortedLayers = new ArrayList<Layer>(layersAvailable);
        Collections.sort(sortedLayers, Technology.LAYERS_BY_HEIGHT_LIFT_CONTACTS);
        float[] backgroundComps = (new Color(User.getColorBackground())).getRGBColorComponents(null);
        float bRed = backgroundComps[0];
        float bGreen = backgroundComps[1];
        float bBlue = backgroundComps[2];
        for(Layer layer : sortedLayers) {
            if (!layer.isVisible()) continue;
            if (layer == Generic.tech.glyphLay && !patternedDisplay) continue;
            Color color = new Color(layer.getGraphics().getRGB());
            float[] compArray = color.getRGBComponents(null);
            float red = compArray[0];
            float green = compArray[1];
            float blue = compArray[2];
            float opacity = (float)layer.getGraphics().getOpacity();
            if (opacity <= 0) continue;
            float inverseAlpha = 1 - opacity;
            if (alphaBlendingOvercolor) {
                red -= bRed*inverseAlpha;
                green -= bGreen*inverseAlpha;
                blue -= bBlue*inverseAlpha;
            } else {
                red *= opacity;
                green *= opacity;
                blue *= opacity;
            }
            layerColors.add(new LayerColor(layer, red, green, blue, inverseAlpha));
        }
        final LayerTab layerTab = getWindowFrame().getLayersTab();
        final boolean showOpacity = true;
        if (layerTab != null)
            SwingUtilities.invokeLater(new Runnable() { public void run() { layerTab.setDisplayAlgorithm(showOpacity); }});
            return layerColors;
    }
    
    public void testJogl() {
        if (drawing instanceof LayerDrawing.Drawing)
            ((LayerDrawing.Drawing)drawing).testJogl();
    }
    
    void opacityChanged() {
        drawing.opacityChanged();
    }
    
	// ************************************* SIMULATION CROSSPROBE LEVEL DISPLAY *************************************

	private static class CrossProbe
	{
		boolean isLine;
		Point2D start, end;
		Rectangle2D box;
		Color color;
	};

	private List<CrossProbe> crossProbeObjects = new ArrayList<CrossProbe>();

	/**
	 * Method to clear the list of cross-probed levels in this EditWindow.
	 * Cross-probed levels are displays of the current simulation value
	 * at a point in the display, and come from the Waveform Window.
	 */
	public void clearCrossProbeLevels()
	{
		crossProbeObjects.clear();
	}

	/**
	 * Method to tell whether there is any cross-probed data in this EditWindow.
	 * Cross-probed levels are displays of the current simulation value
	 * at a point in the display, and come from the Waveform Window.
	 * @return true if there are any cross-probed data displays in this EditWindow.
	 */
	public boolean hasCrossProbeData()
	{
		if (crossProbeObjects.size() > 0) return true;
		return false;
	}

	/**
	 * Method to add a line to the list of cross-probed levels in this EditWindow.
	 * Cross-probed levels are displays of the current simulation value
	 * at a point in the display, and come from the Waveform Window.
	 * @param start the starting point of the line.
	 * @param end the ending point of the line.
	 * @param color the color of the line.
	 */
	public void addCrossProbeLine(Point2D start, Point2D end, Color color)
	{
		CrossProbe cp = new CrossProbe();
		cp.isLine = true;
		cp.start = start;
		cp.end = end;
		cp.color = color;
		crossProbeObjects.add(cp);
	}

	/**
	 * Method to add a box to the list of cross-probed levels in this EditWindow.
	 * Cross-probed levels are displays of the current simulation value
	 * at a point in the display, and come from the Waveform Window.
	 * @param box the bounds of the box.
	 * @param color the color of the box.
	 */
	public void addCrossProbeBox(Rectangle2D box, Color color)
	{
		CrossProbe cp = new CrossProbe();
		cp.isLine = false;
		cp.box = box;
		cp.color = color;
		crossProbeObjects.add(cp);
	}

	private void showCrossProbeLevels(Graphics g)
	{
		for(CrossProbe cp : crossProbeObjects)
		{
			g.setColor(cp.color);
			if (cp.isLine)
			{
				// draw a line
				Point pS = databaseToScreen(cp.start);
				Point pE = databaseToScreen(cp.end);
				g.drawLine(pS.x, pS.y, pE.x, pE.y);
			} else
			{
				// draw a box
				Point pS = databaseToScreen(cp.box.getMinX(), cp.box.getMinY());
				Point pE = databaseToScreen(cp.box.getMaxX(), cp.box.getMaxY());
				int lX = Math.min(pS.x, pE.x);
				int lY = Math.min(pS.y, pE.y);
				int wid = Math.abs(pS.x - pE.x);
				int hei = Math.abs(pS.y - pE.y);
				g.fillRect(lX, lY, wid, hei);
			}
		}
	}

	// ************************************* DRAG BOX *************************************

	public boolean isDoingAreaDrag() { return doingAreaDrag; }

	public void setDoingAreaDrag() { doingAreaDrag = true; }

	public void clearDoingAreaDrag() { doingAreaDrag = false; }

	public Point getStartDrag() { return startDrag; }

	public void setStartDrag(int x, int y) { startDrag.setLocation(x, y); }

	public Point getEndDrag() { return endDrag; }

	public void setEndDrag(int x, int y) { endDrag.setLocation(x, y); }

	private void showDragBox(Graphics g)
	{
		int lX = (int)Math.min(startDrag.getX(), endDrag.getX());
		int hX = (int)Math.max(startDrag.getX(), endDrag.getX());
		int lY = (int)Math.min(startDrag.getY(), endDrag.getY());
		int hY = (int)Math.max(startDrag.getY(), endDrag.getY());
		Graphics2D g2 = (Graphics2D)g;
		g2.setStroke(selectionLine);
		g.setColor(new Color(User.getColorHighlight()));
		g.drawLine(lX, lY, lX, hY);
		g.drawLine(lX, hY, hX, hY);
		g.drawLine(hX, hY, hX, lY);
		g.drawLine(hX, lY, lX, lY);
	}

	// ************************************* CELL FRAME *************************************

	/**
	 * Method to render the cell frame directly to the Graphics.
	 */
	private void drawCellFrame(Graphics g)
	{
		DisplayedFrame df = new DisplayedFrame(cell, g, this);
		df.renderFrame();
	}

	/**
	 * Class for rendering a cell frame.
	 * Extends Cell.FrameDescription and provides hooks for drawing to a Graphics.
	 */
	private static class DisplayedFrame extends Cell.FrameDescription
	{
		private Graphics g;
		private EditWindow wnd;
		private Color lineColor, textColor;

		/**
		 * Constructor for cell frame rendering.
		 * @param cell the Cell that is having a frame drawn.
		 * @param g the Graphics to which to draw the frame.
		 * @param wnd the EditWindow in which this is being drawn.
		 */
		public DisplayedFrame(Cell cell, Graphics g, EditWindow wnd)
		{
			super(cell, wnd.pageNumber);
			this.g = g;
			this.wnd = wnd;
			lineColor = new Color(User.getColorInstanceOutline());
			textColor = new Color(User.getColorText());
		}

		/**
		 * Method to draw a line in a frame.
		 * @param from the starting point of the line (in database units).
		 * @param to the ending point of the line (in database units).
		 */
		public void showFrameLine(Point2D from, Point2D to)
		{
			g.setColor(lineColor);
			Point f = wnd.databaseToScreen(from);
			Point t = wnd.databaseToScreen(to);
			g.drawLine(f.x, f.y, t.x, t.y);
		}

		/**
		 * Method to draw text in a frame.
		 * @param ctr the anchor point of the text.
		 * @param size the size of the text (in database units).
		 * @param maxWid the maximum width of the text (ignored if zero).
		 * @param maxHei the maximum height of the text (ignored if zero).
		 * @param string the text to be displayed.
		 */
		public void showFrameText(Point2D ctr, double size, double maxWid, double maxHei, String string)
		{
			// convert text size to screen points
			Point2D sizeVector = wnd.deltaDatabaseToScreen(size, size);
			int initialHeight = (int)Math.abs(sizeVector.getY());

			// get the font
			Font font = new Font(User.getDefaultFont(), Font.PLAIN, initialHeight);
			g.setFont(font);
			g.setColor(textColor);
			FontRenderContext frc = new FontRenderContext(null, true, true);

			// convert the message to glyphs
			GlyphVector gv = font.createGlyphVector(frc, string);
			LineMetrics lm = font.getLineMetrics(string, frc);
			Rectangle rect = gv.getOutline(0, (float)(lm.getAscent()-lm.getLeading())).getBounds();
			double width = rect.width;
			double height = lm.getHeight();
			Point2D databaseSize = wnd.deltaScreenToDatabase((int)width, (int)height);
			double dbWidth = Math.abs(databaseSize.getX());
			double dbHeight = Math.abs(databaseSize.getY());
			if (maxWid > 0 && maxHei > 0 && (dbWidth > maxWid || dbHeight > maxHei))
			{
				double scale = Math.min(maxWid / dbWidth, maxHei / dbHeight);
				font = new Font(User.getDefaultFont(), Font.PLAIN, (int)(initialHeight*scale));
				if (font != null)
				{
					gv = font.createGlyphVector(frc, string);
					lm = font.getLineMetrics(string, frc);
					rect = gv.getOutline(0, (float)(lm.getAscent()-lm.getLeading())).getBounds();
					width = rect.width;
					height = lm.getHeight();
				}
			}

			// render the text
			Graphics2D g2 = (Graphics2D)g;
			Point p = wnd.databaseToScreen(ctr);
			g2.drawGlyphVector(gv, (float)(p.x - width/2), (float)(p.y + height/2 - lm.getDescent()));
		}
	}

	// ************************************* GRID *************************************

	/**
	 * Method to set the display of a grid in this window.
	 * @param showGrid true to show the grid.
	 */
	public void setGrid(boolean showGrid)
	{
		this.showGrid = showGrid;
		repaintContents(null, false);
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
		double lowX = Math.min(low.getX(), high.getX());
		double lowY = Math.min(low.getY(), high.getY());
		double sizeX = Math.abs(high.getX()-low.getX());
		double sizeY = Math.abs(high.getY()-low.getY());
		Rectangle2D bounds = new Rectangle2D.Double(lowX, lowY, sizeX, sizeY);
		return bounds;
	}

	/**
	 * Method to display the grid.
	 * THIS METHOD IS NOT USED ANYMORE.  THE GRID IS NOW DRAWN INTO THE OFFSCREEN IMAGE.
	 * SEE PixelDrawing.drawGrid()
	 */
	private void drawGrid(Graphics g)
	{
		// grid spacing
		if (gridXSpacing == 0 || gridYSpacing == 0) return;
		double spacingX = gridXSpacing;
		double spacingY = gridYSpacing;
		double boldSpacingX = spacingX * User.getDefGridXBoldFrequency();
		double boldSpacingY = spacingY * User.getDefGridYBoldFrequency();
		double boldSpacingThreshX = spacingX / 4;
		double boldSpacingThreshY = spacingY / 4;

		// screen extent
		Rectangle2D displayable = displayableBounds();
		double lX = displayable.getMinX();  double lY = displayable.getMaxY();
		double hX = displayable.getMaxX();  double hY = displayable.getMinY();
		double scaleX = sz.width / (hX - lX);
		double scaleY = sz.height / (lY - hY);

		// initial grid location
		double x1 = DBMath.toNearest(lX, spacingX);
		double y1 = DBMath.toNearest(lY, spacingY);

		// adjust grid placement according to scale
		boolean allBoldDots = false;
		if (spacingX * scaleX < 5 || spacingY * scaleY < 5)
		{
			// normal grid is too fine: only show the "bold dots"
			x1 = DBMath.toNearest(x1, boldSpacingX);   spacingX = boldSpacingX;
			y1 = DBMath.toNearest(y1, boldSpacingY);   spacingY = boldSpacingY;

			// if even the bold dots are too close, don't draw a grid
			if (spacingX * scaleX < 10 || spacingY * scaleY < 10) return;
		} else if (spacingX * scaleX > 75 && spacingY * scaleY > 75)
		{
			// if zoomed-out far enough, show all bold dots
			allBoldDots = true;
		}

		// draw the grid
		g.setColor(new Color(User.getColorGrid()));
		for(double i = y1; i > hY; i -= spacingY)
		{
			double boldValueY = i;
			if (i < 0) boldValueY -= boldSpacingThreshY/2; else
				boldValueY += boldSpacingThreshY/2;
			boolean everyTenY = Math.abs(boldValueY) % boldSpacingY < boldSpacingThreshY;
			for(double j = x1; j < hX; j += spacingX)
			{
				Point xy = databaseToScreen(j, i);
				int x = xy.x;
				int y = xy.y;
				if (y < 0 || y > sz.height) continue;
				double boldValueX = j;
				if (j < 0) boldValueX -= boldSpacingThreshX/2; else
					boldValueX += boldSpacingThreshX/2;
				boolean everyTenX = Math.abs(boldValueX) % boldSpacingX < boldSpacingThreshX;
				if (allBoldDots && everyTenX && everyTenY)
				{
					g.fillRect(x-2,y, 5, 1);
					g.fillRect(x,y-2, 1, 5);
					g.fillRect(x-1,y-1, 3, 3);
					continue;
				}

				// special case every 10 grid points in each direction
				if (allBoldDots || (everyTenX && everyTenY))
				{
					g.fillRect(x-1,y, 3, 1);
					g.fillRect(x,y-1, 1, 3);
					continue;
				}

				// just a single dot
				g.fillRect(x,y, 1, 1);
			}
		}
		if (User.isGridAxesShown())
		{
			Point xy = databaseToScreen(0, 0);
			if (xy.x >= 0 && xy.x < sz.width)
				g.drawLine(xy.x, 0, xy.x, sz.height-1);
			if (xy.y >= 0 && xy.y < sz.height)
				g.drawLine(0, xy.y, sz.width-1, xy.y);
		}
	}

	// *************************** SEARCHING FOR TEXT ***************************

    /** Information about String search */              private StringSearch textSearch = new StringSearch();
    private static class StringSearch implements Serializable
    {
	/** list of all found strings in the cell */		private List<StringsInCell> foundInCell;
	/** the currently reported string */				private StringsInCell currentStringInCell;
	/** the currently reported string index */			private int currentFindPosition;

        private static Pattern getPattern(String search, boolean caseSensitive)
        {
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE;
            Pattern p = null;

            try
            {
                p = Pattern.compile(search, flags);
            }
            catch (Exception e)
            {
                System.out.println("Error in regular expression '" + search + "'");
                System.out.println(e.getMessage());
            }
            return p;
        }

        private void searchTextNodes(Cell cell, String search, boolean caseSensitive,
                                     boolean regExp, Set whatToSearch, Pattern pattern)
        {
            boolean doTemp = whatToSearch.contains(TextUtils.WhatToSearch.TEMP_NAMES);
            TextUtils.WhatToSearch what = get(whatToSearch, TextUtils.WhatToSearch.NODE_NAME);
            TextUtils.WhatToSearch whatVar = get(whatToSearch, TextUtils.WhatToSearch.NODE_VAR);
            for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
            {
                NodeInst ni = it.next();
                if (what!=null)
                {
                    Name name = ni.getNameKey();
                    if (doTemp || !name.isTempname())
                    {
                        findAllMatches(ni, NodeInst.NODE_NAME, 0, name.toString(), search, caseSensitive, regExp, pattern);
                    }
                }
                if (whatVar!=null)
                {
                    addVariableTextToList(ni, search, caseSensitive,regExp, pattern);
                }
            }
        }

        private void searchTextArcs(Cell cell, String search, boolean caseSensitive,
                                    boolean regExp, Set whatToSearch, Pattern pattern)
        {
            boolean doTemp = whatToSearch.contains(TextUtils.WhatToSearch.TEMP_NAMES);
            TextUtils.WhatToSearch what = get(whatToSearch, TextUtils.WhatToSearch.ARC_NAME);
            TextUtils.WhatToSearch whatVar = get(whatToSearch, TextUtils.WhatToSearch.ARC_VAR);
            for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
            {
                ArcInst ai = it.next();
                if (what!=null)
                {
                    Name name = ai.getNameKey();
                    if (doTemp || !name.isTempname())
                    {
                        findAllMatches(ai, ArcInst.ARC_NAME, 0, name.toString(),
                                search, caseSensitive, regExp, pattern);
                    }
                }
                if (whatVar!=null) {
                    addVariableTextToList(ai, search, caseSensitive, regExp, pattern);
                }
            }
        }

        private void searchTextExports(Cell cell, String search, boolean caseSensitive,
                                       boolean regExp, Set whatToSearch, Pattern pattern)
        {
            WhatToSearch what = get(whatToSearch, TextUtils.WhatToSearch.EXPORT_NAME);
            TextUtils.WhatToSearch whatVar = get(whatToSearch, TextUtils.WhatToSearch.EXPORT_VAR);
            for(Iterator<Export> it = cell.getExports(); it.hasNext(); )
            {
                Export pp = it.next();
                if (what!=null) {
                    Name name = pp.getNameKey();
                    findAllMatches(pp, Export.EXPORT_NAME, 0, name.toString(), search, caseSensitive, regExp, pattern);
                }
                if (whatVar!=null) {
                    addVariableTextToList(pp, search, caseSensitive, regExp, pattern);
                }
            }
        }
        private void searchTextCellVars(Cell cell, String search, boolean caseSensitive,
                                        boolean regExp, Set whatToSearch, Pattern pattern)
        {
            WhatToSearch whatVar = get(whatToSearch, TextUtils.WhatToSearch.CELL_VAR);
            if (whatVar!=null) {
                for(Iterator<Variable> it = cell.getVariables(); it.hasNext(); )
                {
                    Variable var = it.next();
                    if (!var.isDisplay()) continue;
                    findAllMatches(null, var.getKey(), -1, var.getPureValue(-1),
                            search, caseSensitive, regExp, pattern);
                }
            }
        }


        /**
         * Method to change a string to another.
         * @param foundInCell list of string to search
         * @param sic the string being replaced.
         * @param rep the new string.
         * @param cell the Cell in which these strings reside.
         */
        private static void changeOneText(List<StringsInCell> foundInCell, StringsInCell sic, String rep, Cell cell)
        {
            if (sic.replaced) return;
            sic.replaced = true;
            String oldString = sic.theLine;
            String newString;
            if (sic.regExpSearch!=null) {
                Pattern p = Pattern.compile(sic.regExpSearch);
                Matcher m = p.matcher(oldString);
                boolean found = m.find(sic.startPosition);
                LayoutLib.error(!found, "regExp find before replace failed");
                try {
                    StringBuffer ns = new StringBuffer();
                    m.appendReplacement(ns, rep);
                    m.appendTail(ns);
                    newString = ns.toString();
                } catch (Exception e) {
                    System.out.println("Regular expression replace failed");
                    newString = oldString;
                }
            } else {
                newString = oldString.substring(0, sic.startPosition) + rep + oldString.substring(sic.endPosition);
            }
            printChange(sic, newString);
            if (sic.object == null)
            {
                // cell variable name name
                cell.updateVar(sic.key, newString);
            } else
            {
                if (sic.key == NodeInst.NODE_NAME)
                {
                        // node name
                        NodeInst ni = (NodeInst)sic.object;
                        ni.setName(newString);
                }
                else if (sic.key == ArcInst.ARC_NAME)
                {
                        // arc name
                        ArcInst ai = (ArcInst)sic.object;
                        ai.setName(newString);
                }
                else if (sic.key == Export.EXPORT_NAME)
                {
                        // export name
                        Export pp = (Export)sic.object;
                        pp.rename(newString);
                } else
                {
                    // text on a variable
                    ElectricObject base = (ElectricObject)sic.object;
                    Variable var = base.getVar(sic.key);
                    Object obj = var.getObject();
                    if (obj instanceof String)
                    {
                        base.updateVar(sic.key, newString);
                    } else if (obj instanceof String[])
                    {
                        String [] oldLines = (String [])obj;
                        String [] newLines = new String[oldLines.length];
                        for(int i=0; i<oldLines.length; i++)
                        {
                            if (i == sic.lineInVariable) newLines[i] = newString; else
                                newLines[i] = oldLines[i];
                        }
                        base.updateVar(sic.key, newLines);
                    }
                }
            }

            int delta = newString.length() - oldString.length();
            if (delta != 0)
            {
                // because the replacement changes the line length, must update other search strings
                for(StringsInCell oSIC : foundInCell )
                {
                    if (oSIC == sic) continue;
                    if (oSIC.object != sic.object) continue;
                    if (oSIC.key != sic.key) continue;
//                    if (oSIC.name != sic.name) continue;
                    if (oSIC.lineInVariable != sic.lineInVariable) continue;

                    // part of the same string: update it
                    oSIC.theLine = newString;
                    if (oSIC.startPosition > sic.startPosition)
                    {
                        oSIC.startPosition += delta;
                        oSIC.endPosition += delta;
                    }
                }
            }
        }

        private void replaceAllText(String replace, Cell cell)
        {
            int total = 0;
			for(currentFindPosition = 0; currentFindPosition < foundInCell.size(); currentFindPosition++)
			{
				currentStringInCell = foundInCell.get(currentFindPosition);
				changeOneText(foundInCell, currentStringInCell, replace, cell);
				total++;
			}
			if (total == 0)
			{
				Toolkit.getDefaultToolkit().beep();
			} else
			{
				System.out.println("Replaced " + total + " times");
			}
        }

        /**
         * Method to initialize for a new text
         * @param search the string to locate.
         * @param caseSensitive true to match only where the case is the same.
         */
        public void initTextSearch(Cell cell, String search, boolean caseSensitive,
                                   boolean regExp, Set<TextUtils.WhatToSearch> whatToSearch)
        {
            foundInCell = new ArrayList<StringsInCell>();
            if (cell==null)
            {
                System.out.println("No current Cell");
                return;
            }

            Pattern pattern = null;

            if (regExp)
            {
                pattern = getPattern(search, caseSensitive);
                if (pattern == null) return; // errror
            }

            searchTextNodes(cell, search, caseSensitive, regExp, whatToSearch, pattern);
            searchTextArcs(cell, search, caseSensitive, regExp, whatToSearch, pattern);
            searchTextExports(cell, search, caseSensitive, regExp, whatToSearch, pattern);
            searchTextCellVars(cell, search, caseSensitive, regExp, whatToSearch, pattern);
            if (foundInCell.size()==0) System.out.println("Nothing found");
            currentFindPosition = -1;
            currentStringInCell = null;
        }

        /**
         * Method to find the next occurrence of a string.
         * @param reverse true to find in the reverse direction.
         * @return true if something was found.
         */
        private boolean findNextText(Cell cell, Highlighter highlighter, boolean reverse)
        {
            if (foundInCell == null || foundInCell.size() == 0)
            {
                currentStringInCell = null;
                return false;
            }
            if (reverse)
            {
                currentFindPosition--;
                if (currentFindPosition < 0) currentFindPosition = foundInCell.size()-1;
            } else
            {
                currentFindPosition++;
                if (currentFindPosition >= foundInCell.size()) currentFindPosition = 0;
            }
            currentStringInCell = foundInCell.get(currentFindPosition);

            highlighter.clear();

            printFind(currentStringInCell);
            if (currentStringInCell.object == null)
            {
                highlighter.addText(cell, cell, currentStringInCell.key);
            } else
            {
                ElectricObject eObj = (ElectricObject)currentStringInCell.object;
                Variable.Key key = currentStringInCell.key;
                if (eObj instanceof Export) key = Export.EXPORT_NAME;
                else if (eObj instanceof ArcInst) key = ArcInst.ARC_NAME;
                else if (eObj instanceof NodeInst) key = NodeInst.NODE_NAME;
                assert(key != null);
                highlighter.addText(eObj, cell, key);
            }
            highlighter.finished();
            return true;
        }

        /**
         * Method to find all strings on a given database string, and add matches to the list.
         * @param object the Object on which the string resides.
         * @param key the Variable.key on which the string resides.
         * @param lineInVariable the line number in arrayed variables.
         * @param theLine the actual string from the database.
         * @param search the string to find.
         * @param caseSensitive true to do a case-sensitive
         */
        protected void findAllMatches(Object object, Variable.Key key,
                                      int lineInVariable,
                                      String theLine,
                                      String search, boolean caseSensitive,
                                      boolean regExp, Pattern p)
        {
//            int flags =
//                caseSensitive ? 0 : Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE;
            Matcher m = (p != null) ? p.matcher(theLine) : null; // p != null -> regExp

            for(int startPos = 0; ; )
            {
                int endPos;
                if (regExp) {
                    boolean found = m.find();
                    if (!found) break;
                    startPos = m.start();
                    endPos = m.end();
                } else {
                    startPos = TextUtils.findStringInString(theLine, search, startPos, caseSensitive, false);
                    if (startPos < 0) break;
                    endPos = startPos + search.length();
                }
                String regExpSearch = regExp ? search : null;
                foundInCell.add(new StringsInCell(object, key, lineInVariable, theLine,
                                    startPos, endPos, regExpSearch));
                startPos = endPos;
            }
        }

        /**
         * Method to all all displayable variable strings to the list of strings in the Cell.
         * @param eObj the ElectricObject on which variables should be examined.
         * @param search the string to find on the text.
         * @param caseSensitive true to do a case-sensitive
         */
        private void addVariableTextToList(ElectricObject eObj,
                                           String search, boolean caseSensitive,
                                           boolean regExp, Pattern p)
        {
            for(Iterator<Variable> it = eObj.getVariables(); it.hasNext(); )
            {
                Variable var = it.next();
                if (!var.isDisplay()) continue;
                Object obj = var.getObject();
                if (obj instanceof String)
                {
                    findAllMatches(eObj, var.getKey(), -1, (String)obj,
                                   search, caseSensitive, regExp, p);
                } else if (obj instanceof String[])
                {
                    String [] strings = (String [])obj;
                    for(int i=0; i<strings.length; i++)
                    {
                        findAllMatches(eObj, var.getKey(), i, strings[i],
                                       search, caseSensitive, regExp, p);
                    }
                }
            }
        }
    }

	/**
	 * Class to define a string found in a cell.
	 */
	private static class StringsInCell implements Serializable
	{
		/** the object that the string resides on */	final Object object;
		/** the Variable that the string resides on */	final Variable.Key key;
		/** the original string. */						String theLine;
		/** the line number in arrayed variables */		final int lineInVariable;
		/** the starting character position */			int startPosition;
		/** the ending character position */			int endPosition;
		/** the Regular Expression searched for */		final String regExpSearch;
		/** true if the replacement has been done */	boolean replaced;

		StringsInCell(Object object, Variable.Key key,
                      int lineInVariable, String theLine, int startPosition,
                      int endPosition, String regExpSearch)
		{
			this.object = object;
            assert(key!=null);
			this.key = key;
			this.lineInVariable = lineInVariable;
			this.theLine = theLine;
			this.startPosition = startPosition;
			this.endPosition = endPosition;
			this.regExpSearch = regExpSearch;
			this.replaced = false;
		}

		public String toString() { return "StringsInCell obj="+object+" var="+key+
			/*" name="+name+*/" line="+lineInVariable+" start="+startPosition+" end="+endPosition+" msg="+theLine; }
	}

	private static TextUtils.WhatToSearch get(Set whatToSearch, TextUtils.WhatToSearch what) {
		if (whatToSearch.contains(what)) return what;
		return null;
	}

	/**
	 * Method to initialize for a new text
	 * @param search the string to locate.
	 * @param caseSensitive true to match only where the case is the same.
	 */
	public void initTextSearch(String search, boolean caseSensitive,
	                           boolean regExp, Set<TextUtils.WhatToSearch> whatToSearch)
	{
        textSearch.initTextSearch(cell, search, caseSensitive, regExp, whatToSearch);
	}

	private static String repeatChar(char c, int num) {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<num; i++) sb.append(c);
		return sb.toString();
	}

	private static void printFind(StringsInCell sic) {
		String foundHdr = "Found  "+sic.key+": ";
		String foundStr = sic.theLine;
		String highlightHdr = repeatChar(' ', foundHdr.length()+sic.startPosition);
		String highlight =	repeatChar('^', sic.endPosition-sic.startPosition);

		System.out.println(foundHdr+foundStr+"\n"+
						   highlightHdr+highlight);
	}

	/**
	 * Method to find the next occurrence of a string.
	 * @param reverse true to find in the reverse direction.
	 * @return true if something was found.
	 */
	public boolean findNextText(boolean reverse)
	{
        return textSearch.findNextText(cell, highlighter, reverse);
	}

	/**
	 * Method to replace the text that was just selected with findNextText().
	 * @param replace the new text to replace.
	 */
	public void replaceText(String replace)
	{
		if (textSearch.currentStringInCell == null) return;
		ReplaceTextJob job = new ReplaceTextJob(this, replace);
	}

	/**
	 * Method to replace all selected text.
	 * @param replace the new text to replace everywhere.
	 */
	public void replaceAllText(String replace)
	{
		ReplaceAllTextJob job = new ReplaceAllTextJob(this, replace);
	}

    /**
	 * Class to change text in a new thread.
	 */
	private static class ReplaceTextJob extends Job
	{
		private String replace;
        private Cell cell;
		private StringSearch search;

		private ReplaceTextJob(EditWindow wnd, String replace)
		{
			super("Replace Text", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.search = wnd.textSearch;
            this.cell = wnd.cell;
			this.replace = replace;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			StringSearch.changeOneText(search.foundInCell, search.currentStringInCell, replace, cell);
            fieldVariableChanged("search");
            return true;
		}
	}

	/**
	 * Class to change text in a new thread.
	 */
	private static class ReplaceAllTextJob extends Job
	{
		private StringSearch search;
		private String replace;
        private Cell cell;

		public ReplaceAllTextJob(EditWindow wnd, String replace)
		{
			super("Replace All Text", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.search = wnd.textSearch;
			this.replace = replace;
            this.cell = wnd.cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
            search.replaceAllText(replace, cell);
            fieldVariableChanged("search");
			return true;
		}
	}

	private static void printChange(StringsInCell sic, String newString)
	{
		String foundHdr = "Change "+sic.key+": ";
		String foundStr = sic.theLine;
		String replaceHdr = "  ->  ";
		String replaceStr = newString;
		String highlightHdr = repeatChar(' ', foundHdr.length()+sic.startPosition);
		String highlightStr = repeatChar('^', sic.endPosition-sic.startPosition);
		System.out.println(foundHdr+foundStr+replaceHdr+replaceStr+"\n"+
		                   highlightHdr+highlightStr);
	}

    // ************************************* POPUP CLOUD *************************************

    public boolean getShowPopupCloud() { return showPopupCloud; }

    public void setShowPopupCloud(List<String> text, Point2D point)
    {
        showPopupCloud = true;
        popupCloudText = text;
        popupCloudPoint = point;
    }

    public void clearShowPopupCloud() { showPopupCloud = false; }

    private void drawPopupCloud(Graphics2D g)
    {
        // JKG NOTE: disabled for now
        // TODO: decide whether or not this is useful
        /*
        if (popupCloudText == null || popupCloudText.size() == 0) return;
        // draw cloud
        float yspacing = 5;
        float x = (float)popupCloudPoint.getX() + 25;
        float y = (float)popupCloudPoint.getY() + 10 + yspacing;
        for (int i=0; i<popupCloudText.size(); i++) {
            GlyphVector glyph = getFont().createGlyphVector(g.getFontRenderContext(), popupCloudText.get(i));
            g.drawGlyphVector(glyph, x, y);
            y += glyph.getVisualBounds().getHeight() + yspacing;
        }
        */
    }

	// ************************************* WINDOW ZOOM AND PAN *************************************

	/**
	 * Method to return the size of this EditWindow.
	 * @return a Dimension with the size of this EditWindow.
	 */
	public Dimension getScreenSize() { return sz; }

	/**
	 * Method to change the size of this EditWindow.
	 * Also reallocates the offscreen data.
	 */
	public void setScreenSize(Dimension sz)
	{
        logger.entering(CLASS_NAME, "setScreenSize", sz);
		// see if the request must be queued
		if (wf != null)
		{
			synchronized (redrawThese)
			{
				if (runningNow != null)
				{
					// must queue the request
					WindowChangeRequest zap = new WindowChangeRequest();
					zap.wnd = this;
					zap.sz = sz;
					zap.requestType = WindowChangeRequest.RESIZEREQUEST;
					windowChangeRequests.add(zap);
                    logger.exiting(CLASS_NAME, "setScreenSize");
					return;
				}
			}
		}

		this.sz = sz;
		szHalfWidth = sz.width / 2;
		szHalfHeight = sz.height / 2;
		drawing.setScreenSize(getScreenSize());
        logger.exiting(CLASS_NAME, "setScreenSize");
	}

	/**
	 * Method to return the scale factor for this window.
	 * @return the scale factor for this window.
	 */
	public double getScale() { return scale; }

	/**
	 * Method to set the scale factor for this window.
	 * @param scale the scale factor for this window.
	 */
	public void setScale(double scale)
	{
        if (scale <= 0)
            throw new IllegalArgumentException("Negative window scale");
		if (wf != null)
		{
			synchronized (redrawThese)
			{
				if (runningNow != null)
				{
					// must queue the request
					WindowChangeRequest zap = new WindowChangeRequest();
					zap.wnd = this;
					zap.scale = scale;
					zap.requestType = WindowChangeRequest.ZOOMREQUEST;
					windowChangeRequests.add(zap);
					return;
				}
			}
		}
		this.scale = scale;
		removeAllInPlaceTextObjects();
		computeDatabaseBounds();
	}

	private static boolean handleWindowChangeRequests(EditWindow wnd)
	{
		boolean changed = false;
		List<WindowChangeRequest> notThisWindow = null;
		for(WindowChangeRequest zap : windowChangeRequests)
		{
			if (zap.wnd != wnd)
			{
				if (notThisWindow == null) notThisWindow = new ArrayList<WindowChangeRequest>();
				notThisWindow.add(zap);
				continue;
			}
			switch (zap.requestType)
			{
				case WindowChangeRequest.ZOOMREQUEST:
					if (wnd.scale != zap.scale) changed = true;
					wnd.scale = zap.scale;
					break;
				case WindowChangeRequest.PANREQUEST:
					if (wnd.offx != zap.offx || wnd.offy != zap.offy) changed = true;
					wnd.offx = zap.offx;   wnd.offy = zap.offy;
					break;
				case WindowChangeRequest.RESIZEREQUEST:
					wnd.sz = zap.sz;
					wnd.szHalfWidth = wnd.sz.width / 2;
					wnd.szHalfHeight = wnd.sz.height / 2;
					wnd.drawing.setScreenSize(wnd.getScreenSize());
					break;
			}
		}
		if (notThisWindow != null) windowChangeRequests = notThisWindow; else
			windowChangeRequests.clear();
		if (changed)
		{
			wnd.removeAllInPlaceTextObjects();
			wnd.computeDatabaseBounds();
		}
		return changed;
	}

	/**
	 * Method to return the offset factor for this window.
	 * @return the offset factor for this window.
	 */
	public Point2D getOffset() { return new Point2D.Double(offx, offy); }

	/**
	 * Method to return the offset factor for this window.
	 * If new offsets are queued, this gets the ultimate offset value.
	 * @return the offset factor for this window.
	 */
	public Point2D getScheduledOffset()
	{
		double oX = offx;
		double oY = offy;
		synchronized (redrawThese)
		{
			for(WindowChangeRequest zap : windowChangeRequests)
			{
				if (zap.requestType != WindowChangeRequest.PANREQUEST) continue;
				oX = zap.offx;
				oY = zap.offy;
			}
		}
		return new Point2D.Double(oX, oY);
	}

	/**
	 * Method to set the offset factor for this window.
	 * @param off the offset factor for this window.
	 */
	public void setOffset(Point2D off)
	{
		// see if the request must be queued
		if (wf != null)
		{
			synchronized (redrawThese)
			{
				if (runningNow != null)
				{
					// must queue the request
					WindowChangeRequest zap = new WindowChangeRequest();
					zap.wnd = this;
					zap.offx = off.getX();
					zap.offy = off.getY();
					zap.requestType = WindowChangeRequest.PANREQUEST;
					windowChangeRequests.add(zap);
					return;
				}
			}
		}
		offx = off.getX();   offy = off.getY();
		removeAllInPlaceTextObjects();
		computeDatabaseBounds();
	}

	private void setScreenBounds(Rectangle2D bounds)
	{
		double width = bounds.getWidth();
		double height = bounds.getHeight();
		if (width == 0) width = 2;
		if (height == 0) height = 2;
		double scalex = sz.width/width * 0.9;
		double scaley = sz.height/height * 0.9;
		setScale(Math.min(scalex, scaley));
		setOffset(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()));
	}

	private void computeDatabaseBounds()
	{
		double width = sz.width/scale;
		double height = sz.height/scale;
		databaseBounds.setRect(offx - width/2, offy - height/2, width, height);
	}

	public Rectangle2D getDisplayedBounds() { return databaseBounds; }


    private static final double scrollPagePercent = 0.2;
    // ignore programmatic scroll changes. Only respond to user scroll changes
    private boolean ignoreScrollChange = false;
    private static final int scrollRangeMult = 100; // when zoomed in, this prevents rounding from causing problems

    /**
     * New version of setScrollPosition.  Attempts to provides means of scrolling
     * out of cell bounds.
     */
    public void setScrollPosition() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() { setScrollPositionUnsafe(); }
            });
        } else
            setScrollPositionUnsafe();
    }

    /**
     * New version of setScrollPosition.  Attempts to provides means of scrolling
     * out of cell bounds.  This is the Swing unsafe version
     */
    private void setScrollPositionUnsafe()
    {
        if (bottomScrollBar == null || rightScrollBar == null) return;
        bottomScrollBar.setEnabled(cell != null);
        rightScrollBar.setEnabled(cell != null);

        if (cell == null) return;

        Rectangle2D cellBounds = cell.getBounds();
		if (inPlaceDisplay) cellBounds = topLevelCell.getBounds();
        Rectangle2D viewBounds = displayableBounds();

        // get bounds of cell including what is on-screen
        Rectangle2D overallBounds = cellBounds.createUnion(viewBounds);

        // scroll bar is being repositioned: ignore the change events it generates
        ignoreScrollChange = true;

        // adjust scroll bars to reflect new bounds (only if not being adjusted now)
        // newValue, newThumbSize, newMin, newMax
        /*
        if (!bottomScrollBar.getValueIsAdjusting()) {
            bottomScrollBar.getModel().setRangeProperties(
                    (int)((offx-0.5*viewBounds.getWidth())*scrollRangeMult),
                    (int)(viewBounds.getWidth()*scrollRangeMult),
                    (int)((overallBounds.getX() - scrollPagePercent*overallBounds.getWidth())*scrollRangeMult),
                    (int)(((overallBounds.getX()+overallBounds.getWidth()) + scrollPagePercent*overallBounds.getWidth())*scrollRangeMult),
                    false);
            bottomScrollBar.setUnitIncrement((int)(0.05*viewBounds.getWidth()*scrollRangeMult));
            bottomScrollBar.setBlockIncrement((int)(scrollPagePercent*viewBounds.getWidth()*scrollRangeMult));
        }
        //System.out.println("overallBounds="+overallBounds);
        //System.out.println("cellBounds="+cellBounds);
        //System.out.println("offy="+offy);
        System.out.print(" value="+(int)((-offy-0.5*viewBounds.getHeight()))*scrollRangeMult);
        //System.out.print(" extent="+(int)(cellBounds.getHeight()));
        System.out.print(" min="+(int)((-((overallBounds.getY()+overallBounds.getHeight()) + scrollPagePercent*overallBounds.getHeight())))*scrollRangeMult);
        System.out.println(" max="+((int)(-(overallBounds.getY() - scrollPagePercent*overallBounds.getHeight())))*scrollRangeMult);
        if (!rightScrollBar.getValueIsAdjusting()) {
            rightScrollBar.getModel().setRangeProperties(
                    (int)((-offy-0.5*viewBounds.getHeight())*scrollRangeMult),
                    (int)((viewBounds.getHeight())*scrollRangeMult),
                    (int)((-((overallBounds.getY()+overallBounds.getHeight()) + scrollPagePercent*overallBounds.getHeight()))*scrollRangeMult),
                    (int)((-(overallBounds.getY() - scrollPagePercent*overallBounds.getHeight()))*scrollRangeMult),
                    false);
            //System.out.println("model is "+rightScrollBar.getModel());
            rightScrollBar.setUnitIncrement((int)(0.05*viewBounds.getHeight()*scrollRangeMult));
            rightScrollBar.setBlockIncrement((int)(scrollPagePercent*viewBounds.getHeight()*scrollRangeMult));
        }
        */
        double width = (viewBounds.getWidth() < cellBounds.getWidth()) ? viewBounds.getWidth() : cellBounds.getWidth();
        double height = (viewBounds.getHeight() < cellBounds.getHeight()) ? viewBounds.getHeight() : cellBounds.getHeight();

		Point2D dbPt = new Point2D.Double(offx, offy);
//		if (inPlaceDisplay) intoCell.transform(dbPt, dbPt);
		double oX = dbPt.getX();   double oY = dbPt.getY();

		if (!bottomScrollBar.getValueIsAdjusting())
        {
    		int value = (int)((oX-0.5*width)*scrollRangeMult);
    		int extent = (int)(width*scrollRangeMult);
        	int min = (int)((cellBounds.getX() - scrollPagePercent*cellBounds.getWidth())*scrollRangeMult);
        	int max = (int)(((cellBounds.getX()+cellBounds.getWidth()) + scrollPagePercent*cellBounds.getWidth())*scrollRangeMult);
        	bottomScrollBar.getModel().setRangeProperties(value, extent, min, max, false);
            bottomScrollBar.setUnitIncrement((int)(0.05*viewBounds.getWidth()*scrollRangeMult));
            bottomScrollBar.setBlockIncrement((int)(scrollPagePercent*viewBounds.getWidth()*scrollRangeMult));
        }
        if (!rightScrollBar.getValueIsAdjusting())
        {
        	int value = (int)((-oY-0.5*height)*scrollRangeMult);
        	int extent = (int)(height*scrollRangeMult);
        	int min = (int)((-((cellBounds.getY()+cellBounds.getHeight()) + scrollPagePercent*cellBounds.getHeight()))*scrollRangeMult);
        	int max = (int)((-(cellBounds.getY() - scrollPagePercent*cellBounds.getHeight()))*scrollRangeMult);
            rightScrollBar.getModel().setRangeProperties(value, extent, min, max, false);
            rightScrollBar.setUnitIncrement((int)(0.05*viewBounds.getHeight()*scrollRangeMult));
            rightScrollBar.setBlockIncrement((int)(scrollPagePercent*viewBounds.getHeight()*scrollRangeMult));
        }

        ignoreScrollChange = false;
    }

    public void bottomScrollChanged(int value)
    {
        if (cell == null) return;
        if (ignoreScrollChange) return;

		Point2D dbPt = new Point2D.Double(offx, offy);
        double oY = dbPt.getY();

		double val = (double)value/(double)scrollRangeMult;
        Rectangle2D cellBounds = cell.getBounds();
		if (inPlaceDisplay) cellBounds = topLevelCell.getBounds();
        Rectangle2D viewBounds = displayableBounds();
        double width = (viewBounds.getWidth() < cellBounds.getWidth()) ? viewBounds.getWidth() : cellBounds.getWidth();
        double newoffx = val+0.5*width;           // new offset
        //double ignoreDelta = 0.03*viewBounds.getWidth();             // ignore delta
        //double delta = newoffx - offx;
        //if (Math.abs(delta) < Math.abs(ignoreDelta)) return;
        Point2D offset = new Point2D.Double(newoffx, oY);
        setOffset(offset);
		getSavedFocusBrowser().updateCurrentFocus();
        repaintContents(null, false);
    }

    public void rightScrollChanged(int value)
    {
        if (cell == null) return;
        if (ignoreScrollChange) return;

		Point2D dbPt = new Point2D.Double(offx, offy);
		double oX = dbPt.getX();

        double val = (double)value/(double)scrollRangeMult;
        Rectangle2D cellBounds = cell.getBounds();
		if (inPlaceDisplay) cellBounds = topLevelCell.getBounds();
        Rectangle2D viewBounds = displayableBounds();
        double height = (viewBounds.getHeight() < cellBounds.getHeight()) ? viewBounds.getHeight() : cellBounds.getHeight();
        double newoffy = -(val+0.5*height);
        // annoying cause +y is down in java
        //double ignoreDelta = 0.03*viewBounds.getHeight();             // ignore delta
        //double delta = newoffy - offy;
        //if (Math.abs(delta) < Math.abs(ignoreDelta)) return;
        Point2D offset = new Point2D.Double(oX, newoffy);
        setOffset(offset);
		getSavedFocusBrowser().updateCurrentFocus();
        repaintContents(null, false);
    }

	/**
	 * Method to focus the screen so that an area fills it.
	 * @param bounds the area to make fill the screen.
	 */
	public void focusScreen(Rectangle2D bounds)
	{
		if (bounds == null) return;
		if (inPlaceDisplay)
		{
			Point2D llPt = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
			Point2D urPt = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
			outofCell.transform(llPt, llPt);
			outofCell.transform(urPt, urPt);
			double lX = Math.min(llPt.getX(), urPt.getX());
			double hX = Math.max(llPt.getX(), urPt.getX());
			double lY = Math.min(llPt.getY(), urPt.getY());
			double hY = Math.max(llPt.getY(), urPt.getY());
			bounds = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);
		}
		setScreenBounds(bounds);
		setScrollPosition();
		getSavedFocusBrowser().saveCurrentFocus();
		repaintContents(null, false);
	}

	/**
	 * Method to compute the bounds of the cell in this EditWindow.
	 * Bounds includes factors such as frame size and large text.
	 * @return the bounds of the cell in this EditWindow.
	 */
	public Rectangle2D getBoundsInWindow()
	{
        Rectangle2D cellBounds = cell.getBounds();
        Dimension d = new Dimension();
        int frameFactor = Cell.FrameDescription.getCellFrameInfo(cell, d);
        Rectangle2D frameBounds = new Rectangle2D.Double(-d.getWidth()/2, -d.getHeight()/2, d.getWidth(), d.getHeight());
        if (frameFactor == 0)
        {
            cellBounds = frameBounds;
            if (cell.isMultiPage())
            {
            	double offY = pageNumber * Cell.FrameDescription.MULTIPAGESEPARATION;
            	cellBounds.setRect(cellBounds.getMinX(), cellBounds.getMinY() + offY, cellBounds.getWidth(), cellBounds.getHeight());
            }
        } else
        {
            if (cellBounds.getWidth() == 0 && cellBounds.getHeight() == 0)
            {
                int defaultCellSize = 60;
                cellBounds = new Rectangle2D.Double(cellBounds.getCenterX()-defaultCellSize/2,
                    cellBounds.getCenterY()-defaultCellSize/2, defaultCellSize, defaultCellSize);
            }

            // make sure text fits
    		if (wf != null && runningNow != null)
    		{
//    			System.out.println("Warning: screen extent computation is inaccurate");
    		} else
    		{
	            double oldScale = getScale();
	            Point2D oldOffset = getOffset();
	            setScreenBounds(cellBounds);
	            Rectangle2D relativeTextBounds = cell.getRelativeTextBounds(this);
	            if (relativeTextBounds != null)
	            {
	                Rectangle2D newCellBounds = new Rectangle2D.Double();
	                Rectangle2D.union(relativeTextBounds, cellBounds, newCellBounds);
	                cellBounds = newCellBounds;
	            }
	            setScale(oldScale);
	            setOffset(oldOffset);
    		}

            // make sure title box fits (if there is just a title box)
            if (frameFactor == 1)
            {
                Rectangle2D.union(frameBounds, cellBounds, frameBounds);
                cellBounds = frameBounds;
            }
        }
        return cellBounds;
	}

	// Highlighting methods for the EditWindow_ interface
	public void addElectricObject(ElectricObject eObj, Cell cell)
	{
		highlighter.addElectricObject(eObj, cell);
	}

	public Rectangle2D getHighlightedArea()
	{
		return highlighter.getHighlightedArea(this);
	}

	public void addHighlightArea(Rectangle2D rect, Cell cell)
	{
		highlighter.addArea(rect, cell);
	}

	public void addHighlightLine(Point2D pt1, Point2D pt2, Cell cell, boolean thick)
	{
		highlighter.addLine(pt1, pt2, cell, thick);
	}

	public void addHighlightText(ElectricObject eObj, Cell cell, Variable.Key varKey)
	{
		highlighter.addText(eObj, cell, varKey);
	}

	public ElectricObject getOneElectricObject(Class clz) { return highlighter.getOneElectricObject(clz); }

	public List<Geometric> getHighlightedEObjs(boolean wantNodes, boolean wantArcs)
	{
		return highlighter.getHighlightedEObjs(wantNodes, wantArcs);
	}

	public Set<Network> getHighlightedNetworks()
	{
		return highlighter.getHighlightedNetworks();
	}

	public Point2D getHighlightOffset()
	{
		return highlighter.getHighlightOffset();
	}

	public void setHighlightOffset(int dX, int dY)
	{
		highlighter.setHighlightOffset(dX, dY);
	}

	public List<Highlight2> saveHighlightList()
	{
		List<Highlight2> saveList = new ArrayList<Highlight2>();
		for(Highlight2 h : highlighter.getHighlights())
			saveList.add(h);
		return saveList;
	}

	public void restoreHighlightList(List<Highlight2> list)
	{
		highlighter.setHighlightListGeneral(list);
	}

	public void clearHighlighting()
	{
		highlighter.clear();
	}

	public void finishedHighlighting()
	{
		highlighter.finished();
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
                Rectangle2D cellBounds = getBoundsInWindow();
                focusScreen(cellBounds);
                return;
            }
        }
		getSavedFocusBrowser().saveCurrentFocus();
		repaint();
	}

	public void zoomOutContents()
	{
		double scale = getScale();
		setScale(scale / 2);
		getSavedFocusBrowser().saveCurrentFocus();
		repaintContents(null, false);
	}

	public void zoomInContents()
	{
		double scale = getScale();
		setScale(scale * 2);
		getSavedFocusBrowser().saveCurrentFocus();
		repaintContents(null, false);
	}

	public void focusOnHighlighted()
	{
		// focus on highlighting
		Rectangle2D bounds = highlighter.getHighlightedArea(this);
		focusScreen(bounds);
	}

    /**
     * Get the Saved View Browser for this Edit Window
     */
    public EditWindowFocusBrowser getSavedFocusBrowser() { return viewBrowser; }


    // ************************************* HIERARCHY TRAVERSAL *************************************

    /**
     * Get the window's VarContext
     * @return the current VarContext
     */
    public VarContext getVarContext() { return cellVarContext; }

    private PortInst[] pushSelectedPorts(PortInst pi) {
        PortInst[] newSelectedPorts = new PortInst[selectedPorts.length + 1];
        System.arraycopy(selectedPorts, 0, newSelectedPorts, 0, selectedPorts.length);
        newSelectedPorts[selectedPorts.length] = pi;
        return newSelectedPorts;
    }

    private PortInst[] popSelectedPorts() {
        PortInst[] newSelectedPorts = new PortInst[selectedPorts.length - 1];
        System.arraycopy(selectedPorts, 0, newSelectedPorts, 0, newSelectedPorts.length);
        return newSelectedPorts;
    }

    /**
     * Push into an instance (go down the hierarchy)
     * @param keepFocus true to keep the zoom and scale in the new window.
     * @param newWindow true to create a new window for the cell.
     * @param inPlace true to descend "in-place" showing the higher levels.
     */
    public void downHierarchy(boolean keepFocus, boolean newWindow, boolean inPlace)
    {
        // get highlighted
        Highlight2 h = highlighter.getOneHighlight();
        if (h == null) return;
        ElectricObject eobj = h.getElectricObject();

        NodeInst ni = null;
        PortInst pi = null;

        // see if a nodeinst was highlighted (true if text on nodeinst was highlighted)
        if (eobj instanceof NodeInst) ni = (NodeInst)eobj;

        // see if portinst was highlighted
        if (eobj instanceof PortInst)
        {
            pi = (PortInst)eobj;
            ni = pi.getNodeInst();
        }
        if (ni == null)
        {
            System.out.println("Must select a Node to descend into");
            return;
        }
        if (!ni.isCellInstance())
        {
            System.out.println("Can only descend into cell instances");
            return;
        }
        Cell cell = (Cell)ni.getProto();
        Cell schCell = cell.getEquivalent();

        // special case: if cell is icon of current cell, descend into icon
        if (this.cell == schCell) schCell = cell;
        if (schCell == null) schCell = cell;

        // determine display factors for new cell
        WindowFrame.DisplayAttributes da = new WindowFrame.DisplayAttributes();
        da.scale = scale;
        da.offX = offx;
        da.offY = offy;
		if (keepFocus)
		{
			da.offX -= ni.getAnchorCenterX();
			da.offY -= ni.getAnchorCenterY();
		}
		da.inPlace = inPlace;

		// handle in-place display
		if (cell != schCell) da.inPlace = false;
		if (da.inPlace)
		{
			AffineTransform transIn = ni.rotateIn(ni.translateIn());
			AffineTransform transOut = ni.translateOut(ni.rotateOut());
      		da.inPlaceDescent = new ArrayList<NodeInst>();
			if (inPlaceDisplay)
			{
	    		// already doing in-place display: continue the transformation
				da.outofCell = new AffineTransform(outofCell);
				da.outofCell.concatenate(transOut);
				da.intoCell = new AffineTransform(intoCell);
				da.intoCell.preConcatenate(transIn);
	    		for(NodeInst n : inPlaceDescent) da.inPlaceDescent.add(n);
	      		da.topLevelCell = topLevelCell;
	    	} else
	    	{
	    		// first in-place display: setup transformation
	    		da.outofCell = transOut;
	    		da.intoCell = transIn;
	    		da.topLevelCell = this.cell;
	       	}
			da.inPlaceDescent.add(ni);
	    }

		// for stacked NodeInsts, must choose which one
		Nodable desiredNO = null;
		List<Nodable> possibleNodables = new ArrayList<Nodable>();
		Netlist nl = ni.getParent().acquireUserNetlist();
		if (nl == null)
		{
			System.out.println("Netlist is not ready");
			return;
		}
		for(Iterator<Nodable> it = nl.getNodables(); it.hasNext(); )
		{
			Nodable no = it.next();
			if (no.getNodeInst() == ni)
			{
				possibleNodables.add(no);
			}
		}
		if (possibleNodables.size() > 1)
		{
			desiredNO = possibleNodables.get(0);

			// see if there are any waveform windows
			boolean promptUser = isArrayedContextMatter(desiredNO);
			if (promptUser)
			{
				String [] manyOptions = new String[possibleNodables.size()];
				int i = 0;
				for(Nodable no : possibleNodables)
					manyOptions[i++] = no.getName();
		        String chosen = (String)JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Descend into which node?",
		            "Choose a Node", JOptionPane.QUESTION_MESSAGE, null, manyOptions, manyOptions[0]);
		        if (chosen == null) return;
				for(Nodable no : possibleNodables)
				{
					if (no.getName().equals(chosen)) { desiredNO = no;   break; }
				}
			}
		}

        // do the descent
        boolean redisplay = true;
        if (inPlace) redisplay = false;
		if (keepFocus) redisplay = false;
		EditWindow newWND = this;
		if (newWindow)
		{
			WindowFrame newWF = WindowFrame.createEditWindow(schCell);
			newWND = (EditWindow)newWF.getContent();
		}
        else
            SelectObject.selectObjectDialog(schCell, true);

		VarContext vc;
        if (desiredNO != null)
        {
        	vc = cellVarContext.push(desiredNO);
        } else
        {
        	vc = cellVarContext.push(ni);
        }
        newWND.showCell(schCell, vc, redisplay, da);
        newWND.getWindowFrame().addToHistory(schCell, vc, da);
        if (!redisplay) fullRepaint();
		clearSubCellCache();

        // if highlighted was a port inst, then highlight the corresponding export
        if (pi != null)
        {
            Export schExport = schCell.findExport(pi.getPortProto().getName());
            if (schExport != null)
            {
                PortInst origPort = schExport.getOriginalPort();
				newWND.highlighter.addElectricObject(origPort, schCell);
				newWND.highlighter.finished();
            }
        }
    }

    /**
     * Returns true if, in terms of context, it matters which index
     * of the nodable we push into.
     * @param no the nodable
     * @return true if we need to ask the user which index to descent into,
     * false otherwise.
     */
    public static boolean isArrayedContextMatter(Nodable no)
	{
		// matters if the user requested it
		if (User.isPromptForIndexWhenDescending()) return true;

		// matters if there is a waveform window open
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = it.next();
            if (wf.getContent() instanceof WaveformWindow) return true;
        }

		// if getdrive is called
        for (Iterator<Variable> it = no.getVariables(); it.hasNext(); ) {
            Variable var = it.next();
            Object obj = var.getObject();
            if (obj instanceof String) {
                String str = (String)obj;
                if (str.matches(".*LE\\.getdrive.*")) return true;
            }
        }

		// does not matter
		return false;
    }

    /**
     * Pop out of an instance (go up the hierarchy)
     */
    public void upHierarchy()
	{
        if (cell == null) return;
        Cell oldCell = cell;

        // determine which export is selected so it can be shown in the upper level
        Export selectedExport = null;
        Set<Network> nets = highlighter.getHighlightedNetworks();
        for(Network net : nets)
        {
        	for(Iterator<Export> eIt = net.getExports(); eIt.hasNext(); )
        	{
        		Export e = eIt.next();
        		selectedExport = e;
        		break;
        	}
        	if (selectedExport != null) break;
        }

        try {
            Nodable no = cellVarContext.getNodable();
			if (no != null)
			{
		        Cell parent = no.getParent();

				// see if this was in history, if so, restore offset and scale
				// search backwards to get most recent entry
				// search history **before** calling setCell, otherwise we find
				// the history record for the cell we just switched to
				VarContext context = cellVarContext.pop();
				int historyIndex = wf.findCellHistoryIndex(parent, context);
				PortInst pi = null;
				if (historyIndex >= 0)
				{
					// found previous in cell history: show it
					WindowFrame.CellHistory foundHistory = wf.getCellHistoryList().get(historyIndex);
					pi = foundHistory.getDisplayAttributes().selPort;
					foundHistory.setContext(context);
					if (selectedExport != null)
						foundHistory.getDisplayAttributes().selPort = no.getNodeInst().findPortInstFromProto(selectedExport);
					wf.setCellByHistory(historyIndex);
				} else
				{
					// no previous history: make one up
			        WindowFrame.DisplayAttributes da = new WindowFrame.DisplayAttributes();
			        da.scale = scale;
			        da.offX = offx;
			        da.offY = offy;
			        da.inPlace = inPlaceDisplay;
			        da.intoCell = intoCell;
			        da.outofCell = outofCell;
			        da.topLevelCell = topLevelCell;
			        da.inPlaceDescent = null;
			        if (inPlaceDisplay)
			        {
			        	int inPlaceDepth = inPlaceDescent.size() - 1;
			        	if (inPlaceDepth == 0)
			        	{
			        		da.inPlace = false;
			        	} else
			        	{
			        		da.inPlaceDescent = new ArrayList<NodeInst>();
			        		da.intoCell = new AffineTransform();
			        		da.outofCell = new AffineTransform();
			        		for(int i=0; i<inPlaceDepth; i++)
			        		{
			        			NodeInst ni = inPlaceDescent.get(i);
			        			da.inPlaceDescent.add(ni);
			        			da.outofCell.concatenate(ni.translateOut(ni.rotateOut()));
			        			da.intoCell.preConcatenate(ni.rotateIn(ni.translateIn()));
			        		}
			        	}
			        }
					showCell(parent, context, true, da);
			        wf.addToHistory(parent, context, da);

					// highlight node we came from
					if (selectedExport != null)
						pi = no.getNodeInst().findPortInstFromProto(selectedExport);
	                if (pi != null)
	                    highlighter.addElectricObject(pi, parent);
	                else
						highlighter.addElectricObject(no.getNodeInst(), parent);
				}
				clearSubCellCache();

//				// highlight node we came from
//				if (selectedExport != null)
//					pi = no.getNodeInst().findPortInstFromProto(selectedExport);
//                if (pi != null)
//                    highlighter.addElectricObject(pi, parent);
//                else
//					highlighter.addElectricObject(no.getNodeInst(), parent);

                // highlight portinst selected at the time, if any
                SelectObject.selectObjectDialog(parent, true);
				return;
			}

			// no parent - if icon, go to sch view
			if (cell.isIcon())
			{
				Cell schCell = cell.getEquivalent();
				if (schCell != null)
				{
					setCell(schCell, VarContext.globalContext, null);
                    SelectObject.selectObjectDialog(schCell, true);
					return;
				}
			}

			// find all possible parents in all libraries
			Set<Cell> found = new HashSet<Cell>();
			for(Iterator<NodeInst> it = cell.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				Cell parent = ni.getParent();
                if (parent.getLibrary().isHidden()) continue;
				found.add(parent);
			}
			if (cell.isSchematic())
			{
				Cell iconView = cell.iconView();
				if (iconView != null)
				{
					for(Iterator<NodeInst> it = iconView.getInstancesOf(); it.hasNext(); )
					{
						NodeInst ni = it.next();
						if (ni.isIconOfParent()) continue;
						Cell parent = ni.getParent();
                        if (parent.getLibrary().isHidden()) continue;
						found.add(parent);
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
				Cell parent = found.iterator().next();
				setCell(parent, VarContext.globalContext, null);

				// highlight instance
                for (Iterator<NodeInst> it = parent.getNodes(); it.hasNext(); )
                {
                    NodeInst ni = it.next();
                    if (ni.isCellInstance())
                    {
                        Cell nodeCell = (Cell)ni.getProto();
                        if (nodeCell == oldCell || nodeCell.isIconOf(oldCell))
                        {
            				if (selectedExport != null)
            					highlighter.addElectricObject(ni.findPortInstFromProto(selectedExport), parent); else
            						highlighter.addElectricObject(ni, parent);
                            break;
                        }
                    }
                }
                highlighter.finished();
                SelectObject.selectObjectDialog(parent, true);
			} else
			{
				// prompt the user to choose a parent cell

				// BUG #436: The following code creates and displays a popup menu
				// with a list of cell names caused by an ambiguous "Up Hierarchy" command.
				// The popup does not allow the up/down arrows to scroll through it.
				// Unfortunately, this is a known bug in Java's JPopupMenu object.
				// I also tried: JPopupMenu.setDefaultLightWeightPopupEnabled(false);
				JPopupMenu parents = new JPopupMenu("parents");
				for(Cell parent : found)
				{
                    String cellName = parent.describe(false);
					JMenuItem menuItem = new JMenuItem(cellName);
					menuItem.addActionListener(this);
					parents.add(menuItem);
				}
				parents.show(overall, 0, 0);
			}
        } catch (NullPointerException e)
		{
            ActivityLogger.logException(e);
		}
    }

	/**
	 * Method to clear the cache of expanded subcells.
	 * This is used by layer visibility which, when changed, causes everything to be redrawn.
	 */
	public static void clearSubCellCache()
	{
        PixelDrawing.clearSubCellCache();
        LayerDrawing.clearSubCellCache();
	}

	public static void forceRedraw(Cell cell)
	{
        PixelDrawing.forceRedraw(cell);
        LayerDrawing.forceRedraw(cell);
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
		double dbX = (screenX - szHalfWidth) / scale + offx;
		double dbY = (szHalfHeight - screenY) / scale + offy;
		Point2D dbPt = new Point2D.Double(dbX, dbY);

		// if doing in-place display, transform into the proper cell
		if (inPlaceDisplay)
       		intoCell.transform(dbPt, dbPt);

		return dbPt;
	}

    /**
     * Method to convert a rectangle in screen units to a rectangle in
     * database units.
     * @param screenRect the rectangle to convert
     * @return the same rectangle in database units
     */
    public Rectangle2D screenToDatabase(Rectangle2D screenRect)
    {
        Point2D anchor = screenToDatabase((int)screenRect.getX(), (int)screenRect.getY());
        Point2D size = deltaScreenToDatabase((int)screenRect.getWidth(), (int)screenRect.getHeight());
        // note that lower left corner in screen units is upper left corner in db units, so
        // compensate for that here
        return new Rectangle2D.Double(anchor.getX(), anchor.getY()+size.getY(), size.getX(), -size.getY());

    }
	/**
	 * Method to convert a screen distance to a database distance.
	 * @param screenDX the X coordinate change (on the screen in this EditWindow).
	 * @param screenDY the Y coordinate change (on the screen in this EditWindow).
	 * @return the distance in database units.
	 */
	public Point2D deltaScreenToDatabase(int screenDX, int screenDY)
	{
		Point2D origin = screenToDatabase(0, 0);
		Point2D pt = screenToDatabase(screenDX, screenDY);
		return new Point2D.Double(pt.getX() - origin.getX(), pt.getY() - origin.getY());
	}

	/**
	 * Method to convert a database coordinate to screen coordinates.
	 * @param dbX the X coordinate (in database units).
	 * @param dbY the Y coordinate (in database units).
	 * @param result the Point in which to store the screen coordinates.
	 */
	public void databaseToScreen(double dbX, double dbY, Point result)
	{
		// if doing in-place display, transform out of the proper cell
		if (inPlaceDisplay)
		{
			Point2D dbPt = new Point2D.Double(dbX, dbY);
       		outofCell.transform(dbPt, dbPt);
       		dbX = dbPt.getX();
       		dbY = dbPt.getY();
		}
        double scrX = szHalfWidth + (dbX - offx) * scale;
        double scrY = szHalfHeight - (dbY - offy) * scale;
		result.x = (int)(scrX >= 0 ? scrX + 0.5 : scrX - 0.5);
		result.y = (int)(scrY >= 0 ? scrY + 0.5 : scrY - 0.5);
	}

	/**
	 * Method to convert a database coordinate to screen coordinates.
	 * @param dbX the X coordinate (in database units).
	 * @param dbY the Y coordinate (in database units).
	 * @return the coordinate on the screen.
	 */
	public Point databaseToScreen(double dbX, double dbY)
	{
		Point result = new Point();
		databaseToScreen(dbX, dbY, result);
		return result;
	}

	/**
	 * Method to convert a database coordinate to screen coordinates.
	 * @param db the coordinate (in database units).
	 * @return the coordinate on the screen.
	 */
	public Point databaseToScreen(Point2D db)
	{
   		return databaseToScreen(db.getX(), db.getY());
	}

	/**
	 * Method to convert a database rectangle to screen coordinates.
	 * @param db the rectangle (in database units).
	 * @return the rectangle on the screen.
	 */
	public Rectangle databaseToScreen(Rectangle2D db)
	{
		Point llPt = databaseToScreen(db.getMinX(), db.getMinY());
		Point urPt = databaseToScreen(db.getMaxX(), db.getMaxY());
		int screenLX = llPt.x;
		int screenHX = urPt.x;
		int screenLY = llPt.y;
		int screenHY = urPt.y;
		if (screenHX < screenLX) { int swap = screenHX;   screenHX = screenLX; screenLX = swap; }
		if (screenHY < screenLY) { int swap = screenHY;   screenHY = screenLY; screenLY = swap; }
		return new Rectangle(screenLX, screenLY, screenHX-screenLX+1, screenHY-screenLY+1);
	}

	/**
	 * Method to convert a database distance to a screen distance.
	 * @param dbDX the X change (in database units).
	 * @param dbDY the Y change (in database units).
	 * @return the distance on the screen.
	 */
	public Point deltaDatabaseToScreen(double dbDX, double dbDY)
	{
		Point origin = databaseToScreen(0, 0);
		Point pt = databaseToScreen(dbDX, dbDY);
		return new Point(pt.x - origin.x, pt.y - origin.y);
	}

	/**
	 * Method to snap a point to the nearest database-space grid unit.
	 * @param pt the point to be snapped.
	 */
	public static void gridAlign(Point2D pt)
	{
		DBMath.gridAlign(pt, User.getAlignmentToGrid());
	}

	// ************************************* TEXT *************************************

	/**
	 * Method to find the size in database units for text of a given point size in this EditWindow.
	 * The scale of this EditWindow is used to determine the acutal unit size.
	 * @param pointSize the size of the text in points.
	 * @return the database size (in grid units) of the text.
	 */
	public double getTextUnitSize(double pointSize)
	{
		return pointSize / scale;
	}

	/**
	 * Method to find the size in points (actual screen units) for text of a given database size in this EditWindow.
	 * The scale of this EditWindow is used to determine the acutal screen size.
	 * @param dbSize the size of the text in database grid-units.
	 * @return the screen size (in points) of the text.
	 */
	public double getTextScreenSize(double dbSize)
	{
		return dbSize * scale;
	}

	public static int getDefaultFontSize() { return TextDescriptor.getDefaultFontSize(); }

	/**
	 * Method to get a Font to use for a given TextDescriptor in this EditWindow.
	 * @param descript the TextDescriptor.
	 * @return the Font to use (returns null if the text is too small to display).
	 */
	public Font getFont(TextDescriptor descript)
	{
        return descript != null ? descript.getFont(this, PixelDrawing.MINIMUMTEXTSIZE) : TextDescriptor.getDefaultFont();
	}

	/**
	 * Method to get the height of text given a TextDescriptor in this EditWindow.
	 * @param descript the TextDescriptor.
	 * @return the height of the text.
	 */
	public double getFontHeight(TextDescriptor descript)
	{
		double size = getDefaultFontSize();
		if (descript != null)
			size = descript.getTrueSize(this);
		return size;
	}

	/**
	 * Method to convert a string and descriptor to a GlyphVector.
	 * @param text the string to convert.
	 * @param font the Font to use.
	 * @return a GlyphVector describing the text.
	 */
	public GlyphVector getGlyphs(String text, Font font)
	{
		// make a glyph vector for the desired text
        return TextDescriptor.getGlyphs(text, font);
	}

    public void databaseChanged(DatabaseChangeEvent e)
    {
        // if cell was deleted, set cell to null
        if (cell != null && !cell.isLinked())
        {
        	showCell(null, VarContext.globalContext, true, null);
            wf.cellHistoryGoBack();
        }
    }

    /**
     * Method to export directly PNG file.
	 * @param ep printable object.
     * @param filePath
     */
    public void writeImage(ElectricPrinter ep, String filePath)
    {
        BufferedImage img = getPrintImage(ep);
        PNG.writeImage(img, filePath);
    }

	/**
	 * Method to intialize for printing.
	 * @param ep the ElectricPrinter object.
	 * @param pageFormat information about the print job.
     * @return Always true.
	 */
	public boolean initializePrinting(ElectricPrinter ep, PageFormat pageFormat)
	{
		int scaleFactor = ep.getDesiredDPI() / 72;
		if (scaleFactor > 2) scaleFactor = 2; else
			if (scaleFactor <= 0) scaleFactor = 1;
		int pageWid = (int)pageFormat.getImageableWidth() * scaleFactor;
		int pageHei = (int)pageFormat.getImageableHeight() * scaleFactor;
		overall.setSize(pageWid, pageHei);
		overall.validate();
		overall.repaint();
        return true;
    }

	/**
	 * Method to print window using offscreen canvas.
	 * @param ep printable object.
	 * @return the image to print (null on error).
	 */
	public BufferedImage getPrintImage(ElectricPrinter ep)
	{
		if (getCell() == null) return null;

		int scaleFactor = ep.getDesiredDPI() / 72;
		if (scaleFactor > 2) scaleFactor = 2; else
			if (scaleFactor <= 0) scaleFactor = 1;

		BufferedImage img = ep.getBufferedImage();
		if (img == null)
		{
			// change window size
			int iw = (int)ep.getPageFormat().getImageableWidth() * scaleFactor;
			int ih = (int)ep.getPageFormat().getImageableHeight() * scaleFactor;
			PixelDrawing offscreen = new PixelDrawing(new Dimension(iw, ih));

			// prepare for printing
			PrinterJob pj = ep.getPrintJob();
			ColorSupported cs = pj.getPrintService().getAttribute(ColorSupported.class);
			int printMode = 1;
			if (cs.getValue() == 0) printMode = 2;
			offscreen.setPrintingMode(printMode);
			offscreen.setBackgroundColor(Color.WHITE);
//			int oldBackgroundColor = User.getColorBackground();
//			User.setColorBackground(0xFFFFFF);

			// draw int
            Rectangle2D cellBounds = getBoundsInWindow();
    		double width = cellBounds.getWidth();
        	double height = cellBounds.getHeight();
            if (width == 0) width = 2;
            if (height == 0) height = 2;
            double scalex = sz.width/width * 0.9;
            double scaley = sz.height/height * 0.9;
            double scale = Math.min(scalex, scaley);
    		EPoint offset = new EPoint(cellBounds.getCenterX(), cellBounds.getCenterY());

			offscreen.printImage(scale, offset, getCell(), getVarContext());
			img = offscreen.getBufferedImage();
			ep.setBufferedImage(img);

//			// restore display state
//			offscreen.setPrintingMode(0);
//			User.setColorBackground(oldBackgroundColor);
		}

		// copy the image to the page if graphics is not null
		Graphics2D g2d = (Graphics2D)ep.getGraphics();
		if (g2d != null)
		{
			int ix = (int)ep.getPageFormat().getImageableX() * scaleFactor;
			int iy = (int)ep.getPageFormat().getImageableY() * scaleFactor;
			g2d.scale(1.0 / scaleFactor, 1.0 / scaleFactor);
			g2d.drawImage(img, ix, iy, null);
		}
		return img;
	}

	/**
	 * Method to pan along X or Y according to fixed amount of ticks
	 * @param direction 0 is X and 1 is Y
	 * @param panningAmounts
	 * @param ticks
	 */
	public void panXOrY(int direction, double[] panningAmounts, int ticks)
	{
		Cell cell = getCell();
		if (cell == null) return;
		Dimension dim = getSize();
		double panningAmount = panningAmounts[User.getPanningDistance()];
		double value = (direction == 0) ? dim.width : dim.height;
		int mult = (int)((double)value * panningAmount / getScale());
		if (mult == 0) mult = 1;
		Point2D wndOffset = getOffset();
		Point2D newOffset = (direction == 0) ?
		        new Point2D.Double(wndOffset.getX() - mult*ticks, wndOffset.getY()) :
		        new Point2D.Double(wndOffset.getX(), wndOffset.getY() - mult*ticks);
		setOffset(newOffset);
		getSavedFocusBrowser().updateCurrentFocus();
		repaintContents(null, false);
	}
}
