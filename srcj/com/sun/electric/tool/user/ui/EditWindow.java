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

import com.sun.electric.database.change.DatabaseChangeListener;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.SwingExamineTask;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.HighlightListener;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.FindText.WhatToSearch;
import com.sun.electric.Main;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Toolkit;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * This class defines an editing window for displaying circuitry.
 * It implements WindowContent, which means it can be in the main part of a window
 * (to the right of the explorer panel).
 */
public class EditWindow extends JPanel
	implements WindowContent, MouseMotionListener, MouseListener, MouseWheelListener, KeyListener, ActionListener,
        HighlightListener, DatabaseChangeListener
{
	/** the window scale */									private double scale;
	/** the window offset */								private double offx = 0, offy = 0;
	/** the window bounds in database units */				private Rectangle2D databaseBounds;
	/** the size of the window (in pixels) */				private Dimension sz;
	/** the cell that is in the window */					private Cell cell;
	/** true if doing in-place display */					private boolean inPlaceDisplay;
	/** transform from screen to cell (in-place only) */	private AffineTransform intoCell;
	/** transform from cell to screen (in-place only) */	private AffineTransform outofCell;
	/** top-level cell being displayed (in-place only) */	private Cell topLevelCell;
	/** path to cell being edited (in-place only) */		private List inPlaceDescent;

	/** Cell's VarContext */                                private VarContext cellVarContext;
	/** the window frame containing this editwindow */      private WindowFrame wf;
	/** the offscreen data for rendering */					private PixelDrawing offscreen = null;
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
	/** Strings to write to popup cloud */                  private List popupCloudText;
	/** lower left corner of popup cloud */                 private Point2D popupCloudPoint;

    /** Highlighter for this window */                      private Highlighter highlighter;
    /** Mouse-over Highlighter for this window */           private Highlighter mouseOverHighlighter;

	/** list of windows to redraw (gets synchronized) */	private static List redrawThese = new ArrayList();
	/** true if rendering a window now (synchronized) */	private static EditWindow runningNow = null;

	private static final int SCROLLBARRESOLUTION = 200;

    /** for drawing selection boxes */	private static final BasicStroke selectionLine = new BasicStroke(
        	1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {2}, 3);
    /** for outlining down-hierarchy in-place bounds */	private static final BasicStroke inPlaceMarker = new BasicStroke(3);

	// ************************************* CONSTRUCTION *************************************

    // constructor
    private EditWindow(Cell cell, WindowFrame wf)
	{
        this.cell = cell;
        this.wf = wf;
		this.gridXSpacing = User.getDefGridXSpacing();
		this.gridYSpacing = User.getDefGridYSpacing();
		inPlaceDisplay = false;

		sz = new Dimension(500, 500);
		setSize(sz.width, sz.height);
		setPreferredSize(sz);
		databaseBounds = new Rectangle2D.Double();

        cellHistory = new ArrayList();
        cellHistoryLocation = -1;
        scale = 1;

		// the total panel in the waveform window
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
		setOpaque(false);
		setLayout(null);

		//setAutoscrolls(true);
        // add listeners --> BE SURE to remove listeners in finished()
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
        highlighter = new Highlighter(Highlighter.SELECT_HIGHLIGHTER, wf);
        highlighter.addHighlightListener(this);
        highlighter.addHighlightListener(WaveformWindow.getStaticHighlightListener());
        mouseOverHighlighter = new Highlighter(Highlighter.MOUSEOVER_HIGHLIGHTER, wf);
        mouseOverHighlighter.addHighlightListener(this);
        Undo.addDatabaseChangeListener(this);
		if (wf != null) setCell(cell, VarContext.globalContext);
	}

	/**
	 * Factory method to create a new EditWindow with a given cell, in a given WindowFrame.
	 * @param cell the cell in this EditWindow.
	 * @param wf the WindowFrame that this EditWindow lives in.
	 * @return the new EditWindow.
	 */
	public static EditWindow CreateElectricDoc(Cell cell, WindowFrame wf)
	{
		EditWindow ui = new EditWindow(cell, wf);
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
		setCell(cell, VarContext.globalContext);
        // Highlight an instance of cell we came from in current cell
        highlighter.clear();
        for (Iterator it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = (NodeInst)it.next();
            if (ni.getProto() instanceof Cell) {
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
		MessagesWindow.userCommandIssued();
		lastXPosition = evt.getX();   lastYPosition = evt.getY();
		EditWindow wnd = (EditWindow)evt.getSource();

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

			StatusBar.setCoordinates("(" + TextUtils.formatDouble(pt.getX(), 2) + ", " + TextUtils.formatDouble(pt.getY(), 2) + ")", wnd.wf);
		}
	}

	// the MouseWheelListener events
	public void mouseWheelMoved(MouseWheelEvent evt) { WindowFrame.curMouseWheelListener.mouseWheelMoved(evt); }

	// the KeyListener events
	public void keyPressed(KeyEvent evt)
	{
		MessagesWindow.userCommandIssued();
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

	// ************************************* INFORMATION *************************************

	/**
	 * Method to return the top-level JPanel for this EditWindow.
	 * The actual EditWindow object is below the top level, surrounded by scroll bars.
	 * @return the top-level JPanel for this EditWindow.
	 */
	public JPanel getPanel() { return overall; }

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

	/**
	 * Method to return the cell that is shown in this window.
	 * @return the cell that is shown in this window.
	 */
	public Cell getCell() { return cell; }

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
	public List getInPlaceEditNodePath() { return inPlaceDescent; }

	/**
	 * Method to set the List of NodeInsts to the cell being in-place edited.
	 * In-place display implies that the user has descended into a lower-level
	 * cell while requesting that the upper-level remain displayed.
	 * @param list the List of NodeInsts to the cell being in-place edited.
	 */
	public void setInPlaceEditNodePath(List list) { inPlaceDescent = list; }

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
	 * Method to return the WindowFrame in which this EditWindow resides.
	 * @return the WindowFrame in which this EditWindow resides.
	 */
	public WindowFrame getWindowFrame() { return wf; }

	/**
	 * Method to set the cell that is shown in the window to "cell".
	 */
	public void setCell(Cell cell, VarContext context)
	{
		// by default record history and fillscreen
		// However, when navigating through history, don't want to record new
		// history objects.
        if (context == null) context = VarContext.globalContext;
		setCell(cell, context, true, true);
	}

	/**
	 * Method to set the cell that is shown in the window to "cell".
	 */
	private void setCell(Cell cell, VarContext context, boolean addToHistory, boolean fillTheScreen)
	{
		// record current history before switching to new cell
		saveCurrentCellHistoryState();

		// set new values
		this.cell = cell;
		this.cellVarContext = context;
        if (cell != null) {
            Library lib = cell.getLibrary();
            lib.setCurCell(cell);
        }
		//Library curLib = Library.getCurrent();
		//curLib.setCurCell(cell);
		highlighter.clear();
		highlighter.finished();
        mouseOverHighlighter.clear();
        mouseOverHighlighter.finished();

		setWindowTitle();
		if (wf != null)
		{
			if (cell != null)
			{
				if (wf == WindowFrame.getCurrentWindowFrame(false))
				{
					// if auto-switching technology, do it
					PaletteFrame.autoTechnologySwitch(cell);
				}
			}
		}
		if (fillTheScreen) fillScreen();

		if (addToHistory) {
			addToHistory(cell, context);
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
		wf.setTitle(wf.composeTitle(cell, ""));
	}

	/**
	 * Method to find an EditWindow that is displaying a given cell.
	 * @param cell the Cell to find.
	 * @return the EditWindow showing that cell, or null if none found.
	 */
	public static EditWindow findWindow(Cell cell)
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			if (content.getCell() == cell) return (EditWindow)content;
		}
		return null;
	}

	/**
	 * Method to return the PixelDrawing object that represents the offscreen image
	 * of the Cell in this EditWindow.
	 * @return the offscreen object for this window.
	 */
	public PixelDrawing getOffscreen() { return offscreen; }

	public void loadExplorerTree(DefaultMutableTreeNode rootNode)
	{
		wf.libraryExplorerNode = ExplorerTree.makeLibraryTree();
		wf.jobExplorerNode = Job.getExplorerTree();
		wf.errorExplorerNode = ErrorLogger.getExplorerTree();
		wf.signalExplorerNode = null;
		rootNode.add(wf.libraryExplorerNode);
		rootNode.add(wf.jobExplorerNode);
		rootNode.add(wf.errorExplorerNode);
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
		removeKeyListener(this);
		removeMouseListener(this);
		removeMouseMotionListener(this);
		removeMouseWheelListener(this);
        highlighter.removeHighlightListener(this);
		highlighter.removeHighlightListener(WaveformWindow.getStaticHighlightListener());
        mouseOverHighlighter.removeHighlightListener(this);
        Undo.removeDatabaseChangeListener(this);
        highlighter.delete();
        mouseOverHighlighter.delete();
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

	/**
	 * Method to repaint this EditWindow.
	 * Composites the image (taken from the PixelDrawing object)
	 * with the grid, highlight, and any dragging rectangle.
	 */
	public void paint(Graphics g)
	{
		// to enable keys to be received
		if (wf == WindowFrame.getCurrentWindowFrame())
			requestFocus();

		// redo the explorer tree if it changed
		//wf.redoExplorerTreeIfRequested();

		if (offscreen == null || !getSize().equals(sz))
		{
			setScreenSize(getSize());
			repaintContents(null);
			return;
		}

		// show the image
		Image img = offscreen.getImage();
		synchronized(img) { g.drawImage(img, 0, 0, this); };

		// overlay other things if there is a valid cell
		if (cell != null)
		{
			// add in grid if requested
			if (showGrid) drawGrid(g);

			// add cross-probed level display
			showCrossProbeLevels(g);

            if (Main.getDebug()) {
				// add in highlighting
	            if (Job.acquireExamineLock(false)) {
	                try {
	                    // add in the frame if present
	                    drawCellFrame(g);
	
	                    //long start = System.currentTimeMillis();
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
				Graphics2D g2 = (Graphics2D)g;
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
				g2.fill(outerArea);

				// draw a red box around the cell being edited
				g2.setStroke(inPlaceMarker);
				g.setColor(Color.RED);
				g.drawLine(i1.x, i1.y, i2.x, i2.y);
				g.drawLine(i2.x, i2.y, i3.x, i3.y);
				g.drawLine(i3.x, i3.y, i4.x, i4.y);
				g.drawLine(i4.x, i4.y, i1.x, i1.y);
			}
		}

		// draw any components that are on top (such as in-line text edits)
		super.paint(g);
	}

	public void fullRepaint() { repaintContents(null); }

	/**
	 * Method requests that every EditWindow be redrawn, including a rerendering of its contents.
	 */
	public static void repaintAllContents()
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)content;
			wnd.repaintContents(null);
		}
	}

	/**
	 * Method requests that every EditWindow be redrawn, without rerendering the offscreen contents.
	 */
	public static void repaintAll()
	{
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			EditWindow wnd = (EditWindow)content;
			wnd.repaint();
		}
	}

	/**
	 * Method requests that this EditWindow be redrawn, including a rerendering of the contents.
	 */
	public void repaintContents(Rectangle2D bounds)
	{
		// start rendering thread
		if (offscreen == null) return;

		// do the redraw in a separate thread
		synchronized(redrawThese)
		{
			if (runningNow != null)
			{
				if (runningNow != this && !redrawThese.contains(this))
					redrawThese.add(this);
				return;
			}
			runningNow = this;
		}
		RenderJob renderJob = new RenderJob(this, offscreen, bounds);

        // do the redraw in the main thread
        setScrollPosition();                        // redraw scroll bars
	}

	/**
	 * This class queues requests to rerender a window.
	 */
	private static class RenderJob extends Job
	{
		private EditWindow wnd;
		private PixelDrawing offscreen;
		private Rectangle2D bounds;

		protected RenderJob(EditWindow wnd, PixelDrawing offscreen, Rectangle2D bounds)
		{
			super("Display", User.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.wnd = wnd;
			this.offscreen = offscreen;
			this.bounds = bounds;
			startJob();
		}

		public boolean doIt()
		{
			// do the hard work of re-rendering the image
            //long start = System.currentTimeMillis();
            try
            {
				offscreen.drawImage(bounds);
            } catch (java.util.ConcurrentModificationException e)
            {
				System.out.println("GOT ConcurrentModificationException during redisplay!");
            	ActivityLogger.logException(e);
				wnd.repaintContents(bounds);
            }
            //long end = System.currentTimeMillis();
            //System.out.println("Rerender time "+TextUtils.getElapsedTime(end-start));

			// see if anything else is queued
			synchronized(redrawThese)
			{
				if (redrawThese.size() > 0)
				{
					runningNow = (EditWindow)redrawThese.get(0);
					redrawThese.remove(0);
					RenderJob nextJob = new RenderJob(runningNow, runningNow.getOffscreen(), null);
					return true;
				}
				runningNow = null;
			}
			wnd.repaint();
			return true;
		}
	}

	/**
	 * Special "hook" to render a single node.
	 * This is used by the PaletteWindow to draw nodes that aren't really in the database.
	 */
	public Image renderNode(NodeInst ni, double scale)
	{
		offscreen.clearImage(false);
		setScale(scale);
		offscreen.drawNode(ni, DBMath.MATID, true, null);
		return offscreen.composite();
	}

	/**
	 * Special "hook" to render a single arc.
	 * This is used by the PaletteWindow to draw arcs that aren't really in the database.
	 */
	public Image renderArc(ArcInst ai, double scale)
	{
		offscreen.clearImage(false);
		setScale(scale);
		offscreen.drawArc(ai, DBMath.MATID);
		return offscreen.composite();
	}

	// ************************************* SIMULATION CROSSPROBE LEVEL DISPLAY *************************************

	private static class CrossProbe
	{
		boolean isLine;
		Point2D start, end;
		Rectangle2D box;
		Color color;
	};

	private List crossProbeObjects = new ArrayList();

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
		for(Iterator it = crossProbeObjects.iterator(); it.hasNext(); )
		{
			CrossProbe cp = (CrossProbe)it.next();
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
		g.setColor(Color.white);
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
	public static class DisplayedFrame extends Cell.FrameDescription
	{
		private Graphics g;
		private EditWindow wnd;

		/**
		 * Constructor for cell frame rendering.
		 * @param cell the Cell that is having a frame drawn.
		 * @param g the Graphics to which to draw the frame.
		 * @param wnd the EditWindow in which this is being drawn.
		 */
		public DisplayedFrame(Cell cell, Graphics g, EditWindow wnd)
		{
			super(cell);
			this.g = g;
			this.wnd = wnd;
		}

		/**
		 * Method to initialize the drawing of a frame.
		 */
		public void renderInit()
		{
			g.setColor(Color.BLACK);
		}

		/**
		 * Method to draw a line in a frame.
		 * @param from the starting point of the line (in database units).
		 * @param to the ending point of the line (in database units).
		 */
		public void showFrameLine(Point2D from, Point2D to)
		{
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
		repaint();
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
			int y = (int)((lY - i) * scaleY);
			if (y < 0 || y > sz.height) continue;
			double boldValueY = i;
			if (i < 0) boldValueY -= boldSpacingThreshY/2; else
				boldValueY += boldSpacingThreshY/2;
			boolean everyTenY = Math.abs(boldValueY) % boldSpacingY < boldSpacingThreshY;
			for(double j = x1; j < hX; j += spacingX)
			{
				int x = (int)((j-lX) * scaleX);
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
	}

	// *************************** SEARCHING FOR TEXT ***************************

	/** list of all found strings in the cell */		private List foundInCell;
	/** the currently reported string */				private StringsInCell currentStringInCell;
	/** the currently reported string index */			private int currentFindPosition;

	/**
	 * Class to define a string found in a cell.
	 */
	private static class StringsInCell
	{
		/** the object that the string resides on */	final Object object;
		/** the Variable that the string resides on */	final Variable.Key key;
		/** the Name that the string resides on */		final Name name;
		/** the original string. */						String theLine;
		/** the line number in arrayed variables */		final int lineInVariable;
		/** the starting character position */			int startPosition;
		/** the ending character position */			int endPosition;
		/** the Regular Expression searched for */		final String regExpSearch;
		/** descriptor for String's object */			final WhatToSearch what; 
		/** true if the replacement has been done */	boolean replaced;

		StringsInCell(Object object, Variable.Key key, Name name, 
		              int lineInVariable, String theLine, int startPosition, 
		              int endPosition, String regExpSearch, WhatToSearch what)
		{
			this.object = object;
			this.key = key;
			this.name = name;
			this.lineInVariable = lineInVariable;
			this.theLine = theLine;
			this.startPosition = startPosition;
			this.endPosition = endPosition;
			this.regExpSearch = regExpSearch;
			this.what = what;
			this.replaced = false;
		}

		public String toString() { return "StringsInCell obj="+object+" var="+key+
			" name="+name+" line="+lineInVariable+" start="+startPosition+" end="+endPosition+" msg="+theLine; }
	}
	
	private WhatToSearch get(Set whatToSearch, WhatToSearch what) {
		if (whatToSearch.contains(what)) return what;
		return null;
	}
	
	private void searchTextNodes(String search, boolean caseSensitive,
		                         boolean regExp, Set whatToSearch) {
		boolean doTemp = whatToSearch.contains(WhatToSearch.TEMP_NAMES);
		WhatToSearch what = get(whatToSearch, WhatToSearch.NODE_NAME);
		WhatToSearch whatVar = get(whatToSearch, WhatToSearch.NODE_VAR);
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (what!=null) 
			{
				Name name = ni.getNameKey();
				if (doTemp || !name.isTempname()) 
				{
					findAllMatches(ni, null, 0, name, name.toString(), 
								   foundInCell, search, caseSensitive, 
								   regExp, what);
				}
			}
			if (whatVar!=null) 
			{
				addVariableTextToList(ni, foundInCell, search, caseSensitive, 
									  regExp, whatVar);
			}
		}
    }

	private void searchTextArcs(String search, boolean caseSensitive,
							    boolean regExp, Set whatToSearch) {
		boolean doTemp = whatToSearch.contains(WhatToSearch.TEMP_NAMES);
		WhatToSearch what = get(whatToSearch, WhatToSearch.ARC_NAME);
		WhatToSearch whatVar = get(whatToSearch, WhatToSearch.ARC_VAR);
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (what!=null) 
			{
				Name name = ai.getNameKey();
				if (doTemp || !name.isTempname())
				{
					findAllMatches(ai, null, 0, name, name.toString(), 
								   foundInCell, search, caseSensitive, 
								   regExp, what);
				}
			}
			if (whatVar!=null) {
				addVariableTextToList(ai, foundInCell, search, caseSensitive, 
									  regExp, whatVar);
			}
		}
	}

	private void searchTextExports(String search, boolean caseSensitive,
								   boolean regExp, Set whatToSearch) {
		WhatToSearch what = get(whatToSearch, WhatToSearch.EXPORT_NAME);
		WhatToSearch whatVar = get(whatToSearch, WhatToSearch.EXPORT_VAR);
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			if (what!=null) {
				Name name = pp.getNameKey();
				findAllMatches(pp, null, 0, null, name.toString(), foundInCell, 
							   search, caseSensitive, regExp, what);
			}
			if (whatVar!=null) {
				addVariableTextToList(pp, foundInCell, search, caseSensitive, 
									  regExp, whatVar);
			}
		}
	}
	private void searchTextCellVars(String search, boolean caseSensitive,
									boolean regExp, Set whatToSearch) {
		WhatToSearch whatVar = get(whatToSearch, WhatToSearch.CELL_VAR);
		if (whatVar!=null) {
			for(Iterator it = cell.getVariables(); it.hasNext(); )
			{
				Variable var = (Variable)it.next();
				if (!var.isDisplay()) continue;
				findAllMatches(null, var, -1, null, var.getPureValue(-1), 
							   foundInCell, search, caseSensitive, regExp, 
							   whatVar);
			}
		}
	}
	/**
	 * Method to initialize for a new text search.
	 * @param search the string to locate.
	 * @param caseSensitive true to match only where the case is the same.
	 */
	public void initTextSearch(String search, boolean caseSensitive,
	                           boolean regExp, Set whatToSearch)
	{
		foundInCell = new ArrayList();
		if (cell==null) 
		{
			System.out.println("No current Cell");
			return;
		}
		searchTextNodes(search, caseSensitive, regExp, whatToSearch); 
		searchTextArcs(search, caseSensitive, regExp, whatToSearch); 
		searchTextExports(search, caseSensitive, regExp, whatToSearch);
		searchTextCellVars(search, caseSensitive, regExp, whatToSearch);
		if (foundInCell.size()==0) System.out.println("Nothing found");
		currentFindPosition = -1;
		currentStringInCell = null;
	}
	
	private String repeatChar(char c, int num) {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<num; i++) sb.append(c);
		return sb.toString(); 
	}

	private void printFind(StringsInCell sic) {
		String foundHdr = "Found  "+sic.what+": ";
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
		currentStringInCell = (StringsInCell)foundInCell.get(currentFindPosition);

		highlighter.clear();

		printFind(currentStringInCell);
		if (currentStringInCell.object == null)
		{
			highlighter.addText(cell, cell, (Variable)currentStringInCell.object, null);
		} else
		{
			ElectricObject eObj = (ElectricObject)currentStringInCell.object;
			Variable var = eObj.getVar(currentStringInCell.key);
			highlighter.addText(eObj, cell, var, currentStringInCell.name);
		}
		highlighter.finished();
		return true;		
	}

	/**
	 * Method to replace the text that was just selected with findNextText().
	 * @param replace the new text to replace.
	 */
	public void replaceText(String replace)
	{
		if (currentStringInCell == null) return;
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
	 * Method to all all displayable variable strings to the list of strings in the Cell.
	 * @param eObj the ElectricObject on which variables should be examined.
	 * @param foundInCell the list of strings found in the cell.
	 * @param search the string to find on the text.
	 * @param caseSensitive true to do a case-sensitive search.
	 */
	private void addVariableTextToList(ElectricObject eObj, List foundInCell, 
	                                   String search, boolean caseSensitive,
	                                   boolean regExp, WhatToSearch what)
	{
		for(Iterator it = eObj.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			if (!var.isDisplay()) continue;
			Object obj = var.getObject();
			if (obj instanceof String)
			{
				findAllMatches(eObj, var, -1, null, (String)obj, foundInCell, 
				               search, caseSensitive, regExp, what);
			} else if (obj instanceof String[])
			{
				String [] strings = (String [])obj;
				for(int i=0; i<strings.length; i++)
				{
					findAllMatches(eObj, var, i, null, strings[i], foundInCell, 
					               search, caseSensitive, regExp, what);
				}
			}
		}
	}

	/**
	 * Method to find all strings on a given database string, and add matches to the list.
	 * @param object the Object on which the string resides.
	 * @param variable the Variable on which the string resides.
	 * @param lineInVariable the line number in arrayed variables.
	 * @param name the name on which the string resides.
	 * @param theLine the actual string from the database.
	 * @param foundInCell the list of found strings.
	 * @param search the string to find.
	 * @param caseSensitive true to do a case-sensitive search.
	 */
	private static void findAllMatches(Object object, Variable variable, 
	                                   int lineInVariable, Name name, 
	                                   String theLine, List foundInCell,
		                               String search, boolean caseSensitive,
		                               boolean regExp, WhatToSearch what)
	{
		int flags = 
			caseSensitive ? 0 : Pattern.CASE_INSENSITIVE+Pattern.UNICODE_CASE;
		Pattern p = regExp ? Pattern.compile(search, flags) : null;
		Matcher m = regExp ? p.matcher(theLine) : null;
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
			Variable.Key key = null;
			if (variable != null) key = variable.getKey();
			foundInCell.add(
			  new StringsInCell(object, key, name, lineInVariable, theLine,
			                    startPos, endPos, regExpSearch, what));
			startPos = endPos;
		}
	}

	/**
	 * Class to change text in a new thread.
	 */
	private static class ReplaceTextJob extends Job
	{
		EditWindow wnd;
		String replace;

		public ReplaceTextJob(EditWindow wnd, String replace)
		{
			super("Replace Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.wnd = wnd;
			this.replace = replace;
			startJob();
		}

		public boolean doIt()
		{
			wnd.changeOneText(wnd.currentStringInCell, replace, wnd.cell);
			return true;
		}
	}

	/**
	 * Class to change text in a new thread.
	 */
	private static class ReplaceAllTextJob extends Job
	{
		EditWindow wnd;
		String replace;

		public ReplaceAllTextJob(EditWindow wnd, String replace)
		{
			super("Replace All Text", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.wnd = wnd;
			this.replace = replace;
			startJob();
		}

		public boolean doIt()
		{
			int total = 0;
			for(wnd.currentFindPosition = 0; wnd.currentFindPosition < wnd.foundInCell.size(); wnd.currentFindPosition++)
			{
				wnd.currentStringInCell = (StringsInCell)wnd.foundInCell.get(wnd.currentFindPosition);
				wnd.changeOneText(wnd.currentStringInCell, replace, wnd.cell);
				total++;
			}
			if (total == 0)
			{
				Toolkit.getDefaultToolkit().beep();
			} else
			{
				System.out.println("Replaced " + total + " times");
			}
			return true;
		}
	}

	private void printChange(StringsInCell sic, String newString) 
	{
		String foundHdr = "Change "+sic.what+": ";
		String foundStr = sic.theLine;
		String replaceHdr = "  ->  ";
		String replaceStr = newString;
		String highlightHdr = repeatChar(' ', foundHdr.length()+sic.startPosition);
		String highlightStr = repeatChar('^', sic.endPosition-sic.startPosition); 
		System.out.println(foundHdr+foundStr+replaceHdr+replaceStr+"\n"+
		                   highlightHdr+highlightStr);
	}

	/**
	 * Method to change a string to another.
	 * @param sic the string being replaced.
	 * @param rep the new string.
	 * @param cell the Cell in which these strings reside.
	 */
	private void changeOneText(StringsInCell sic, String rep, Cell cell)
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
			if (sic.key == null)
			{
				if (sic.name == null)
				{
					// export name
					Export pp = (Export)sic.object;
					pp.rename(newString);
					Undo.redrawObject(pp.getOriginalPort().getNodeInst());					
				} else
				{
					// node or arc name
					Geometric geom = (Geometric)sic.object;
					geom.setName(newString);
					Undo.redrawObject(geom);					
				}
			} else
			{
				// text on a variable
				ElectricObject base = (ElectricObject)sic.object;
				Variable var = base.getVar(sic.key);
				Object obj = var.getObject();
				Variable newVar = null;
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
			for(Iterator it = foundInCell.iterator(); it.hasNext(); )
			{
				StringsInCell oSIC = (StringsInCell)it.next();
				if (oSIC == sic) continue;
				if (oSIC.object != sic.object) continue;
				if (oSIC.key != sic.key) continue;
				if (oSIC.name != sic.name) continue;
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

    // ************************************* POPUP CLOUD *************************************

    public boolean getShowPopupCloud() { return showPopupCloud; }

    public void setShowPopupCloud(List text, Point2D point)
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
            GlyphVector glyph = getFont().createGlyphVector(g.getFontRenderContext(), (String)popupCloudText.get(i));
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
		this.sz = sz;
		offscreen = new PixelDrawing(this);
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
		this.scale = scale;
		computeDatabaseBounds();
	}

	/**
	 * Method to return the offset factor for this window.
	 * @return the offset factor for this window.
	 */
	public Point2D getOffset() { return new Point2D.Double(offx, offy); }

	/**
	 * Method to set the offset factor for this window.
	 * @param off the offset factor for this window.
	 */
	public void setOffset(Point2D off)
	{
		offx = off.getX();   offy = off.getY();
		computeDatabaseBounds();
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
        bottomScrollBar.setEnabled(cell != null);
        rightScrollBar.setEnabled(cell != null);

        if (cell == null) return;

        Rectangle2D cellBounds = cell.getBounds();
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

        if (!bottomScrollBar.getValueIsAdjusting()) {
            bottomScrollBar.getModel().setRangeProperties(
                    (int)((offx-0.5*width)*scrollRangeMult),
                    (int)(width*scrollRangeMult),
                    (int)((cellBounds.getX() - scrollPagePercent*cellBounds.getWidth())*scrollRangeMult),
                    (int)(((cellBounds.getX()+cellBounds.getWidth()) + scrollPagePercent*cellBounds.getWidth())*scrollRangeMult),
                    false);
            bottomScrollBar.setUnitIncrement((int)(0.05*viewBounds.getWidth()*scrollRangeMult));
            bottomScrollBar.setBlockIncrement((int)(scrollPagePercent*viewBounds.getWidth()*scrollRangeMult));
        }
        if (!rightScrollBar.getValueIsAdjusting()) {
            rightScrollBar.getModel().setRangeProperties(
                    (int)((-offy-0.5*height)*scrollRangeMult),
                    (int)((height)*scrollRangeMult),
                    (int)((-((cellBounds.getY()+cellBounds.getHeight()) + scrollPagePercent*cellBounds.getHeight()))*scrollRangeMult),
                    (int)((-(cellBounds.getY() - scrollPagePercent*cellBounds.getHeight()))*scrollRangeMult),
                    false);
            //System.out.println("model is "+rightScrollBar.getModel());
            rightScrollBar.setUnitIncrement((int)(0.05*viewBounds.getHeight()*scrollRangeMult));
            rightScrollBar.setBlockIncrement((int)(scrollPagePercent*viewBounds.getHeight()*scrollRangeMult));
        }

        ignoreScrollChange = false;
    }

    public void bottomScrollChanged(int value)
    {
        if (cell == null) return;
        if (ignoreScrollChange) return;

        double val = (double)value/(double)scrollRangeMult;
        Rectangle2D cellBounds = cell.getBounds();
        Rectangle2D viewBounds = displayableBounds();
        double width = (viewBounds.getWidth() < cellBounds.getWidth()) ? viewBounds.getWidth() : cellBounds.getWidth();
        double newoffx = val+0.5*width;           // new offset
        //double ignoreDelta = 0.03*viewBounds.getWidth();             // ignore delta
        //double delta = newoffx - offx;
        //System.out.println("Old offx="+offx+", new offx="+newoffx+", delta="+(newoffx-offx));
        //System.out.println("will ignore delta offset of "+ignoreDelta);
        //if (Math.abs(delta) < Math.abs(ignoreDelta)) return;
        Point2D offset = new Point2D.Double(newoffx, offy);
        setOffset(offset);
        repaintContents(null);
    }

    public void rightScrollChanged(int value)
    {
        if (cell == null) return;
        if (ignoreScrollChange) return;

        double val = (double)value/(double)scrollRangeMult;
        Rectangle2D cellBounds = cell.getBounds();
        Rectangle2D viewBounds = displayableBounds();
        double height = (viewBounds.getHeight() < cellBounds.getHeight()) ? viewBounds.getHeight() : cellBounds.getHeight();
        double newoffy = -(val+0.5*height);
        // annoying cause +y is down in java
        //double ignoreDelta = 0.03*viewBounds.getHeight();             // ignore delta
        //double delta = newoffy - offy;
        //System.out.println("Old offy="+offy+", new offy="+newoffy+", deltay="+(newoffy-offy));
        //System.out.println("will ignore delta offset of "+ignoreDelta);
        //if (Math.abs(delta) < Math.abs(ignoreDelta)) return;
        Point2D offset = new Point2D.Double(offx, newoffy);
        setOffset(offset);
        repaintContents(null);
    }

	private void setScreenBounds(Rectangle2D bounds)
	{
		double width = bounds.getWidth();
		double height = bounds.getHeight();
		if (width == 0) width = 2;
		if (height == 0) height = 2;
		double scalex = sz.width/width * 0.9;
		double scaley = sz.height/height * 0.9;
		scale = Math.min(scalex, scaley);
		offx = bounds.getCenterX();
		offy = bounds.getCenterY();
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
		computeDatabaseBounds();
		repaintContents(null);
	}

	/**
	 * Method to pan and zoom the screen so that the entire cell is displayed.
	 */
	public void fillScreen()
	{
        EditWindow wnd = this;
        if (wnd.cell != null)
        {
            if (!wnd.cell.getView().isTextView())
            {
                Rectangle2D cellBounds = wnd.cell.getBounds();
                Dimension d = new Dimension();
                int frameFactor = Cell.FrameDescription.getCellFrameInfo(wnd.cell, d);
                Rectangle2D frameBounds = new Rectangle2D.Double(-d.getWidth()/2, -d.getHeight()/2, d.getWidth(), d.getHeight());
                if (frameFactor == 0)
                {
                    cellBounds = frameBounds;
                } else
                {
                    if (cellBounds.getWidth() == 0 && cellBounds.getHeight() == 0)
                    {
                        int defaultCellSize = 60;
                        cellBounds = new Rectangle2D.Double(cellBounds.getCenterX()-defaultCellSize/2,
                                cellBounds.getCenterY()-defaultCellSize/2, defaultCellSize, defaultCellSize);
                    }

                    // make sure text fits
                    wnd.setScreenBounds(cellBounds);
                    Rectangle2D relativeTextBounds = wnd.cell.getRelativeTextBounds(wnd);
                    if (relativeTextBounds != null)
                    {
                        Rectangle2D newCellBounds = new Rectangle2D.Double();
                        Rectangle2D.union(relativeTextBounds, cellBounds, newCellBounds);
                        cellBounds = newCellBounds;
                    }

                    // make sure title box fits (if there is just a title box)
                    if (frameFactor == 1)
                    {
                        Rectangle2D.union(frameBounds, cellBounds, frameBounds);
                        cellBounds = frameBounds;
                    }
                }
                wnd.focusScreen(cellBounds);
                return;
            }
        }
        wnd.repaint();
	}

	public void zoomOutContents()
	{
		double scale = getScale();
		setScale(scale / 2);
		repaintContents(null);
	}

	public void zoomInContents()
	{
		double scale = getScale();
		setScale(scale * 2);
		repaintContents(null);
	}

	public void focusOnHighlighted()
	{
		// focus on highlighting
		Rectangle2D bounds = highlighter.getHighlightedArea(this);
		focusScreen(bounds);
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
    public void downHierarchy(boolean inPlace)
    {
        // get highlighted
        Highlight h = highlighter.getOneHighlight();
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
        NodeProto np = ni.getProto();
        if (!(np instanceof Cell))
        {
            System.out.println("Can only descend into cell instances");
            return;
        }
        Cell cell = (Cell)np;
        Cell schCell = cell.getEquivalent();

        // special case: if cell is icon of current cell, descend into icon
        if (this.cell == schCell) schCell = cell;
        if (schCell == null) schCell = cell;

        // handle in-place display
		if (np != schCell) inPlace = false;
		if (inPlace)
		{
			AffineTransform transIn = ni.rotateIn(ni.translateIn());
			AffineTransform transOut = ni.translateOut(ni.rotateOut());
			if (inPlaceDisplay)
			{
	    		// already doing in-place display: continue the transformation
	    		outofCell.concatenate(transOut);
	    		intoCell.preConcatenate(transIn);
	    	} else
	    	{
	    		// first in-place display: setup transformation
	      		outofCell = transOut;
	      		intoCell = transIn;
	       		topLevelCell = this.cell;
	       		inPlaceDescent = new ArrayList();
	       	}
	   		inPlaceDescent.add(ni);
	    	inPlaceDisplay = true;
	    }

        // do the descent
        boolean redisplay = true;
        if (inPlaceDisplay) redisplay = false;
        if (pi != null)
            setCell(schCell, cellVarContext.push(pi), true, redisplay);
        else
            setCell(schCell, cellVarContext.push(ni), true, redisplay);
        if (!redisplay) fullRepaint();

        // if highlighted was a port inst, then highlight the corresponding export
        if (pi != null)
        {
            Export schExport = schCell.findExport(pi.getPortProto().getName());
            if (schExport != null)
            {
                PortInst origPort = schExport.getOriginalPort();
                highlighter.addElectricObject(origPort, schCell);
                highlighter.finished();
            }
        }
    }

    /**
     * Pop out of an instance (go up the hierarchy)
     */
    public void upHierarchy()
	{
        if (cell == null) return;
        Cell oldCell = cell;
        try {
            Nodable no = cellVarContext.getNodable();
			if (no != null)
			{
		        Cell parent = no.getParent();
		        if (inPlaceDisplay)
		        {
		        	int inPlaceDepth = inPlaceDescent.size() - 1;
		        	if (inPlaceDepth == 0)
		        	{
		        		inPlaceDisplay = false;
		        		inPlaceDescent = null;
		        	} else
		        	{
		        		inPlaceDescent.remove(inPlaceDepth);
		        		intoCell = new AffineTransform();
		        		outofCell = new AffineTransform();
		        		for(int i=0; i<inPlaceDepth; i++)
		        		{
		        			NodeInst ni = (NodeInst)inPlaceDescent.get(i);
	        	    		outofCell.concatenate(ni.translateOut(ni.rotateOut()));
	        	    		intoCell.preConcatenate(ni.rotateIn(ni.translateIn()));
		        		}
		        	}
		        }

				VarContext context = cellVarContext.pop();
				CellHistory foundHistory = null;
				// see if this was in history, if so, restore offset and scale
				// search backwards to get most recent entry
				// search history **before** calling setCell, otherwise we find
				// the history record for the cell we just switched to
				for (int i=cellHistory.size()-1; i>-1; i--) {
					CellHistory history = (CellHistory)cellHistory.get(i);
					if ((history.cell == parent) && (history.context.equals(context))) {
						foundHistory = history;
						break;
					}
				}
                PortInst pi = cellVarContext.getPortInst();
				setCell(parent, context, true, true);
				if (foundHistory != null) {
					setOffset(foundHistory.offset);
					setScale(foundHistory.scale);
				}

				// highlight node we came from
                if (pi != null)
                    highlighter.addElectricObject(pi, parent);
                else
					highlighter.addElectricObject(no.getNodeInst(), parent);
                // highlight portinst selected at the time, if any
				return;
			}

			// no parent - if icon, go to sch view
			if (cell.isIcon())
			{
				Cell schCell = cell.getEquivalent();
				if (schCell != null)
				{
					setCell(schCell, VarContext.globalContext);
					return;
				}
			}            

			// find all possible parents in all libraries
			Set found = new TreeSet();
			for(Iterator it = cell.getInstancesOf(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Cell parent = ni.getParent();
                if (parent.getLibrary().isHidden()) continue;
				found.add(parent);
			}
			if (cell.isSchematic())
			{
				Cell iconView = cell.iconView();
				if (iconView != null)
				{
					for(Iterator it = iconView.getInstancesOf(); it.hasNext(); )
					{
						NodeInst ni = (NodeInst)it.next();
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
				Cell parent = (Cell)found.iterator().next();
				setCell(parent, VarContext.globalContext);
                // highlight instance
                NodeInst highlightNi = null;
                for (Iterator it = parent.getNodes(); it.hasNext(); ){
                    NodeInst ni = (NodeInst)it.next();
                    if (ni.getProto() instanceof Cell) {
                        Cell nodeCell = (Cell)ni.getProto();
                        if (nodeCell == oldCell) {
                            highlighter.addElectricObject(ni, parent);
                            break;
                        }
                        if (nodeCell.isIconOf(oldCell)) {
                            highlighter.addElectricObject(ni, parent);
                            break;
                        }
                    }
                }
                highlighter.finished();
			} else
			{
				// prompt the user to choose a parent cell
				JPopupMenu parents = new JPopupMenu("parents");
				for(Iterator it = found.iterator(); it.hasNext(); )
				{
                    Cell parent = (Cell)it.next();
					String cellName = parent.describe();
					JMenuItem menuItem = new JMenuItem(cellName);
					menuItem.addActionListener(this);
					parents.add(menuItem);
				}
				parents.show(this, 0, 0);
			}
        } catch (NullPointerException e)
		{
            ActivityLogger.logException(e);
		}
    }

    // ************************** Cell History Traversal  *************************************

    /** List of CellHistory objects */                      private List cellHistory;
    /** Location in history (points to valid location) */   private int cellHistoryLocation;
    /** Property name: go back enabled */                   public static final String propGoBackEnabled = "GoBackEnabled";
    /** Property name: go forward enabled */                public static final String propGoForwardEnabled = "GoForwardEnabled";
    /** History limit */                                    private static final int cellHistoryLimit = 20;

    /**
     * Used to track CellHistory and associated values.
     */
    private static class CellHistory
    {
        /** cell */                     public Cell cell;
        /** context */                  public VarContext context;
        /** offset */                   public Point2D offset;
        /** scale */                    public double scale;
        /** highlights */               public List highlights;
        /** highlight offset*/          public Point2D highlightOffset;
    }

    /**
     * Go back in history list.
     */
    public void cellHistoryGoBack() {
        if (cellHistoryLocation <= 0) return;               // at start of history
        setCellByHistory(cellHistoryLocation-1);
    }

    /**
     * Go forward in history list.
     */
    public void cellHistoryGoForward() {
        if (cellHistoryLocation >= (cellHistory.size() - 1)) return; // at end of history
        setCellByHistory(cellHistoryLocation+1);
    }

    /** Returns true if we can go back in history list, false otherwise */
    public boolean cellHistoryCanGoBack() {
        if (cellHistoryLocation > 0) return true;
        return false;
    }

    /** Returns true if we can go forward in history list, false otherwise */
    public boolean cellHistoryCanGoForward() {
        if (cellHistoryLocation < (cellHistory.size() - 1)) return true;
        return false;
    }

    /**
     * Used when new tool bar is created with existing edit window
     * (when moving windows across displays).  Updates back/forward
     * button states.
     */
    public void fireCellHistoryStatus() {
        if (cellHistoryLocation > 0)
            getPanel().firePropertyChange(propGoBackEnabled, false, true);
        if (cellHistoryLocation < (cellHistory.size() - 1))
            getPanel().firePropertyChange(propGoForwardEnabled, false, true);
    }

    /** Adds to cellHistory record list
     * Should only be called via non-history traversing modifications
     * to history. (such as Edit->New Cell).
     */
    private void addToHistory(Cell cell, VarContext context) {

        if (cell == null) return;

        CellHistory history = new CellHistory();
        history.cell = cell;
        history.context = context;

        // when user has moved back through history, and then edits a new cell,
        // get rid of forward history
        if (cellHistoryLocation < (cellHistory.size() - 1)) {
            // inserting into middle of history: get rid of history forward of this
            for (int i=cellHistoryLocation+1; i<cellHistory.size(); i++) {
                cellHistory.remove(i);
            }
            // disable previously enabled forward button
            getPanel().firePropertyChange(propGoForwardEnabled, true, false);
        }

        // if location is 0, adding should enable back button
        if (cellHistoryLocation == 0)
            getPanel().firePropertyChange(propGoBackEnabled, false, true);

        // update history
        cellHistory.add(history);
        cellHistoryLocation = cellHistory.size() - 1;

        // adjust if we are over the limit
        if (cellHistoryLocation > cellHistoryLimit) {
            cellHistory.remove(0);
            cellHistoryLocation--;
        }

        //System.out.println("Adding to History at location="+cellHistoryLocation+", cellHistory.size()="+cellHistory.size());
    }

    /** Records current cell state into history
     * Assumes record pointed to by cellHistoryLocation is
     * history record for the current cell/context.
     */
    private void saveCurrentCellHistoryState() {

        if (cellHistoryLocation < 0) return;

        CellHistory current = (CellHistory)cellHistory.get(cellHistoryLocation);

        //System.out.println("Updating cell history state of location="+cellHistoryLocation+", cell "+cell);

        current.offset = new Point2D.Double(offx, offy);
        current.scale = scale;
        current.highlights = new ArrayList();
        current.highlights.clear();
        for (Iterator it = highlighter.getHighlights().iterator(); it.hasNext(); ) {
            Highlight h = (Highlight)it.next();
            if (h.getCell() == cell)
                current.highlights.add(h);
        }
        current.highlightOffset = highlighter.getHighlightOffset();
    }

    /** Restores cell state from history record */
    private void setCellByHistory(int location) {

        // fire property changes if back/forward buttons should change state
        if (cellHistoryLocation == (cellHistory.size()-1)) {
            // was at end, forward button was disabled
            if (location < (cellHistory.size()-1))
                getPanel().firePropertyChange(propGoForwardEnabled, false, true);
        } else {
            // not at end, forward button was enabled
            if (location == (cellHistory.size()-1))
                getPanel().firePropertyChange(propGoForwardEnabled, true, false);
        }
        if (cellHistoryLocation == 0) {
            // at beginning, back button was disabled
            if (location > 0)
                getPanel().firePropertyChange(propGoBackEnabled, false, true);
        } else {
            // not at beginning, back button was enabled
            if (location == 0)
                getPanel().firePropertyChange(propGoBackEnabled, true, false);
        }

        //System.out.println("Setting cell to location="+location+", cellHistory.size()="+cellHistory.size());

        // get cell history to go to
        CellHistory history = (CellHistory)cellHistory.get(location);

        // see if cell still valid part of database. If not, nullify entry
        if (history.cell == null || !history.cell.isLinked()) {
            history.cell = null;
            history.context = VarContext.globalContext;
            history.offset = new Point2D.Double(0,0);
            history.highlights = new ArrayList();
            history.highlightOffset = new Point2D.Double(0,0);
        }

        // update current cell
        setCell(history.cell, history.context, false, true);
        setOffset(history.offset);
        setScale(history.scale);
        highlighter.setHighlightList(history.highlights);
        highlighter.setHighlightOffset((int)history.highlightOffset.getX(), (int)history.highlightOffset.getY());

        // point to new location *after* calling setCell, since setCell updates by current location
        cellHistoryLocation = location;

        repaintContents(null);
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
	 * @return the coordinate on the screen.
	 */
	public Point databaseToScreen(double dbX, double dbY)
	{
		// if doing in-place display, transform out of the proper cell
		if (inPlaceDisplay)
		{
			Point2D dbPt = new Point2D.Double(dbX, dbY);
       		outofCell.transform(dbPt, dbPt);
       		dbX = dbPt.getX();
       		dbY = dbPt.getY();
		}
		int screenX = (int)Math.round(sz.width/2 + (dbX - offx) * scale);
		int screenY = (int)Math.round(sz.height/2 - (dbY - offy) * scale);
		return new Point(screenX, screenY);
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
		return new Rectangle(screenLX, screenLY, screenHX-screenLX, screenHY-screenLY);
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
		double alignment = User.getAlignmentToGrid();
		long x = Math.round(pt.getX() / alignment);
		long y = Math.round(pt.getY() / alignment);
		pt.setLocation(x * alignment, y * alignment);
	}

	/**
	 * Method to snap a point to the nearest database-space grid unit.
	 * @param pt the point to be snapped.
	 * @param alignment the alignment value to use.
	 */
	public static void gridAlign(Point2D pt, double alignment)
	{
		long x = Math.round(pt.getX() / alignment);
		long y = Math.round(pt.getY() / alignment);
		pt.setLocation(x * alignment, y * alignment);
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

	public static int getDefaultFontSize() { return 14; }

	/**
	 * Method to get a Font to use for a given TextDescriptor in this EditWindow.
	 * @param descript the TextDescriptor.
	 * @return the Font to use (returns null if the text is too small to display).
	 */
	public Font getFont(TextDescriptor descript)
	{
		int size = getDefaultFontSize();
		int fontStyle = Font.PLAIN;
		String fontName = User.getDefaultFont();
		if (descript != null)
		{
			size = (int)descript.getTrueSize(this);
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
		if (size < PixelDrawing.MINIMUMTEXTSIZE) return null;
		Font font = new Font(fontName, fontStyle, size);
		return font;
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
		FontRenderContext frc = new FontRenderContext(null, false, false);
		GlyphVector gv = font.createGlyphVector(frc, text);
		return gv;
	}

    public void databaseEndChangeBatch(Undo.ChangeBatch batch) {
        // if cell was deleted, set cell to null
        if ((cell != null) && !cell.isLinked()) {
            setCell(null, VarContext.globalContext, false, true);
            cellHistoryGoBack();
        }
    }

    public void databaseChanged(Undo.Change evt) {}
    
    public boolean isGUIListener() { return true; }
}
