/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolBar.java
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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.menus.EditMenu;
import com.sun.electric.tool.user.menus.FileMenu;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Iterator;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JToolBar;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;


/**
 * This class manages the Electric toolbar.
 */
public class ToolBar extends JToolBar implements PropertyChangeListener, InternalFrameListener
{
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String selectAreaName = "Area";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String selectObjectsName = "Objects";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String moveFullName = "Full";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String moveHalfName = "Half";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String moveQuarterName = "Quarter";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorClickZoomWireName = "Click/Zoom/Wire";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorSelectName = "Toggle Select";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorWiringName = "Toggle Wiring";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorPanName = "Toggle Pan";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorZoomName = "Toggle Zoom";
	/** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorOutlineName = "Toggle Outline Edit";
	/** Menu name that exists on ToolBar, public for consistency matching */ public static final String cursorMeasureName = "Toggle Measure Distance";
    /** Menu name that exists on ToolBar, public for consistency matching */ public static final String specialSelectName = "Special Select";
	/** Menu name that exists on ToolBar, public for consistency matching */ public static final String OpenLibraryName = "Open Library";
	/** Menu name that exists on ToolBar, public for consistency matching */ public static final String SaveLibraryName = "Save Library";

    /** Go back button */           private JButton goBackButton;
    /** Go forward button */        private JButton goForwardButton;
    /** Undo button */              private JButton undoButton;
    /** Redo button */              private JButton redoButton;
//    /** bookmark button */          private JButton bookmarkButton;
	/** Save button */              private ToolBarButton saveLibraryButton;

    /**
	 * Mode is a typesafe enum class that describes the current editing mode (select, zoom, etc).
	 */
	public static class CursorMode
	{
		private String name;

		private CursorMode(String name) { this.name = name; }

		public String toString() { return "CursorMode="+name; }
        
        /** Describes ClickZoomWire mode (does everything). */  public static final CursorMode CLICKZOOMWIRE = new CursorMode("clickzoomwire");
//		/** Describes Selection mode (click and drag). */		public static final CursorMode SELECT = new CursorMode("select");
//		/** Describes wiring mode (creating arcs). */			public static final CursorMode WIRE = new CursorMode("wire");
		/** Describes Panning mode (move window contents). */	public static final CursorMode PAN = new CursorMode("pan");
		/** Describes Zoom mode (scale window contents). */		public static final CursorMode ZOOM = new CursorMode("zoom");
		/** Describes Outline edit mode. */						public static final CursorMode OUTLINE = new CursorMode("outline");
		/** Describes Measure mode. */							public static final CursorMode MEASURE = new CursorMode("measure");
	}

	/**
	 * Mode is a typesafe enum class that describes the distance that arrow keys move (full, half, or quarter).
	 */
	public static class ArrowDistance
	{
		private String name;
		private double amount;

		private ArrowDistance(String name, double amount)
		{
			this.name = name;
			this.amount = amount;
		}

		public double getDistance() { return amount; }
		public String toString() { return "ArrowDistance="+name; }

		/** Describes full grid unit motion. */				public static final ArrowDistance FULL = new ArrowDistance("full", 1.0);
		/** Describes half grid unit motion. */				public static final ArrowDistance HALF = new ArrowDistance("half", 0.5);
		/** Describes quarter grid unit motion. */			public static final ArrowDistance QUARTER = new ArrowDistance("quarter", 0.25);
	}

	/**
	 * SelectMode is a typesafe enum class that describes the current selection modes (objects or area).
	 */
	public static class SelectMode
	{
		private String name;

		private SelectMode(String name) { this.name = name; }

		public String toString() { return "SelectMode="+name; }

		/** Describes Selection mode (click and drag). */		public static final SelectMode OBJECTS = new SelectMode("objects");
		/** Describes Selection mode (click and drag). */		public static final SelectMode AREA = new SelectMode("area");
	}

	private static CursorMode curMode = CursorMode.CLICKZOOMWIRE;
	private static ArrowDistance curArrowDistance = ArrowDistance.FULL;
	private static SelectMode curSelectMode = SelectMode.OBJECTS;

	public static Cursor zoomCursor = readCursor("CursorZoom.gif", 6, 6);
    public static Cursor zoomOutCursor = readCursor("CursorZoomOut.gif", 6, 6);
	public static Cursor panCursor = readCursor("CursorPan.gif", 8, 8);
	public static Cursor wiringCursor = readCursor("CursorWiring.gif", 0, 0);
	public static Cursor outlineCursor = readCursor("CursorOutline.gif", 0, 0);
	public static Cursor measureCursor = readCursor("CursorMeasure.gif", 0, 0);

    public static final ImageIcon selectSpecialIconOn = Resources.getResource(ToolBar.class, "ButtonSelectSpecialOn.gif");
    public static final ImageIcon selectSpecialIconOff = Resources.getResource(ToolBar.class, "ButtonSelectSpecialOff.gif");
    private static ToolBarButton fullButton, halfButton, quarterButton;    // Access from outside to change selection if
    //  setGridAligment is changed in GridAndAlignmentTab

	private ToolBar() {
        Undo.addPropertyChangeListener(this);
    }

    /**
     * Method to select proper buttom in ToolBar depending on gridAlignment value
     * @param ad
     */
    public static void setGridAligment(double ad)
    {
		if (ad == 1.0) fullButton.setSelected(true); else
		if (ad == 0.5) halfButton.setSelected(true); else
		if (ad == 0.25) quarterButton.setSelected(true);
    }

	/**
	 * Method to create the toolbar.
	 */
	public static ToolBar createToolBar()
	{
		// create the toolbar
		ToolBar toolbar = new ToolBar();
		toolbar.setFloatable(true);
		toolbar.setRollover(true);

        ToolBarButton clickZoomWireButton, panButton, zoomButton, outlineButton, measureButton;
        ToolBarButton objectsButton, areaButton;
        ToolBarButton selectSpecialButton;
        ButtonGroup modeGroup, arrowGroup, selectGroup;

		// the "Select mode" button
		modeGroup = new ButtonGroup();

		/** Open and Save buttons */
        ToolBarButton open = ToolBarButton.newInstance(OpenLibraryName, Resources.getResource(toolbar.getClass(), "ButtonOpenLibrary.gif"));
        open.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.openLibraryCommand(); } });
        open.setToolTipText(OpenLibraryName);
        open.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
        toolbar.add(open);

		toolbar.saveLibraryButton = ToolBarButton.newInstance(SaveLibraryName,
		        Resources.getResource(toolbar.getClass(), "ButtonSaveLibrary.gif"));
        toolbar.saveLibraryButton.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { FileMenu.saveLibraryCommand(Library.getCurrent(), FileType.DEFAULTLIB, false, true); } });
        toolbar.saveLibraryButton.setToolTipText(SaveLibraryName);
        toolbar.saveLibraryButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
        // setModel before setEnable... not sure why yet
		toolbar.saveLibraryButton.setEnabled(Library.getCurrent()!=null);
		toolbar.add(toolbar.saveLibraryButton);

        toolbar.addSeparator();

        clickZoomWireButton = ToolBarButton.newInstance(cursorClickZoomWireName,
            Resources.getResource(toolbar.getClass(), "ButtonClickZoomWire.gif"));
        clickZoomWireButton.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { clickZoomWireCommand(); } });
        clickZoomWireButton.setToolTipText("Click/Zoom/Wire");
        clickZoomWireButton.setSelected(true);
        toolbar.add(clickZoomWireButton);
        modeGroup.add(clickZoomWireButton);

		// the "Pan mode" button
		panButton = ToolBarButton.newInstance(cursorPanName,
            Resources.getResource(toolbar.getClass(), "ButtonPan.gif"));
		panButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { panCommand(); } });
		panButton.setToolTipText(cursorPanName);
		toolbar.add(panButton);
		modeGroup.add(panButton);

		// the "Zoom mode" button
		zoomButton = ToolBarButton.newInstance(cursorZoomName,
            Resources.getResource(toolbar.getClass(), "ButtonZoom.gif"));
		zoomButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { zoomCommand(); } });
		zoomButton.setToolTipText(cursorZoomName);
		toolbar.add(zoomButton);
		modeGroup.add(zoomButton);

		// the "Outline edit mode" button
		outlineButton = ToolBarButton.newInstance(cursorOutlineName,
			Resources.getResource(toolbar.getClass(), "ButtonOutline.gif"));
		outlineButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { outlineEditCommand(); } });
		outlineButton.setToolTipText(cursorOutlineName);
		toolbar.add(outlineButton);
		modeGroup.add(outlineButton);

		// the "Measure mode" button
		measureButton = ToolBarButton.newInstance(cursorMeasureName,
			Resources.getResource(toolbar.getClass(), "ButtonMeasure.gif"));
		measureButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { measureCommand(); } });
		measureButton.setToolTipText(cursorMeasureName);
		toolbar.add(measureButton);
		modeGroup.add(measureButton);

		// setup mode buttons to current setting
		if (curMode == CursorMode.CLICKZOOMWIRE) clickZoomWireButton.setSelected(true); else
			if (curMode == CursorMode.PAN) panButton.setSelected(true); else
				if (curMode == CursorMode.ZOOM) zoomButton.setSelected(true); else
					if (curMode == CursorMode.MEASURE) measureButton.setSelected(true);

		// a separator
		toolbar.addSeparator();

		// the "Full arrow distance" button
		arrowGroup = new ButtonGroup();
		fullButton = ToolBarButton.newInstance(moveFullName,
            Resources.getResource(toolbar.getClass(), "ButtonFull.gif"));
		fullButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { arrowDistanceCommand(ArrowDistance.FULL); } });
		fullButton.setToolTipText("Full motion");
		fullButton.setSelected(true);
		toolbar.add(fullButton);
		arrowGroup.add(fullButton);

		// the "Half arrow distance" button
        halfButton = ToolBarButton.newInstance(moveHalfName,
            Resources.getResource(toolbar.getClass(), "ButtonHalf.gif"));
		halfButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { arrowDistanceCommand(ArrowDistance.HALF); } });
		halfButton.setToolTipText("Half motion");
		toolbar.add(halfButton);
		arrowGroup.add(halfButton);

		// the "Quarter arrow distance" button
		quarterButton = ToolBarButton.newInstance(moveQuarterName,
            Resources.getResource(toolbar.getClass(), "ButtonQuarter.gif"));
		quarterButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { arrowDistanceCommand(ArrowDistance.QUARTER); } });
		quarterButton.setToolTipText("Quarter motion");
		toolbar.add(quarterButton);
		arrowGroup.add(quarterButton);

		// setup arrow buttons to current setting
//		if (curArrowDistance == ArrowDistance.FULL) fullButton.setSelected(true); else
//			if (curArrowDistance == ArrowDistance.HALF) halfButton.setSelected(true); else
//				if (curArrowDistance == ArrowDistance.QUARTER) quarterButton.setSelected(true);
        setGridAligment(User.getAlignmentToGrid());

		// a separator
		toolbar.addSeparator();

        // the "Select Objects" button
		selectGroup = new ButtonGroup();
		objectsButton = ToolBarButton.newInstance(selectObjectsName,
            Resources.getResource(toolbar.getClass(), "ButtonObjects.gif"));
		objectsButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectObjectsCommand(); } });
		objectsButton.setToolTipText("Select Objects");
		objectsButton.setSelected(true);
		toolbar.add(objectsButton);
		selectGroup.add(objectsButton);

        // the "Select Area" button
		areaButton = ToolBarButton.newInstance(selectAreaName,
            Resources.getResource(toolbar.getClass(), "ButtonArea.gif"));
		areaButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectAreaCommand(); } });
		areaButton.setToolTipText("Select Area");
		toolbar.add(areaButton);
		selectGroup.add(areaButton);

		// setup object/area selector to current setting
		if (getSelectMode() == SelectMode.AREA) areaButton.setSelected(true);

		// the "Special select mode" button
        // this is a true toggle button, default is no special select
		ImageIcon initialIcon = selectSpecialIconOff;
		if (getSelectSpecial()) initialIcon = selectSpecialIconOn;
		selectSpecialButton = ToolBarButton.newInstance(specialSelectName, initialIcon);
		selectSpecialButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { toggleSelectSpecialCommand(e); } });
		selectSpecialButton.setToolTipText("Toggle Special Select");
		toolbar.add(selectSpecialButton);

		// a separator
		toolbar.addSeparator();

		// the "Preferences" buttons
		ToolBarButton preferencesButton = ToolBarButton.newInstance("Preferences",
            Resources.getResource(toolbar.getClass(), "ButtonPreferences.gif"));
		preferencesButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { PreferencesFrame.preferencesCommand(); } });
		preferencesButton.setToolTipText("Preferences");
		preferencesButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
        toolbar.add(preferencesButton);

        toolbar.addSeparator();

        // the Undo button
        toolbar.undoButton = new JButton(Resources.getResource(toolbar.getClass(), "ButtonUndo.gif"));
        toolbar.undoButton.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { EditMenu.undoCommand(); } });
        toolbar.undoButton.setToolTipText("Undo");
        toolbar.undoButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
        toolbar.undoButton.setEnabled(Undo.getUndoEnabled());
        toolbar.add(toolbar.undoButton);

        // the Redo button
        toolbar.redoButton = new JButton(Resources.getResource(toolbar.getClass(), "ButtonRedo.gif"));
        toolbar.redoButton.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { EditMenu.redoCommand(); } });
        toolbar.redoButton.setToolTipText("Redo");
        toolbar.redoButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
        toolbar.redoButton.setEnabled(Undo.getRedoEnabled());
        toolbar.add(toolbar.redoButton);

        toolbar.addSeparator();

        // the Cell History button
        toolbar.goBackButton = new JButton(Resources.getResource(toolbar.getClass(), "ButtonGoBack.gif"));
        toolbar.goBackButton.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { goBackButtonCommand(); } });
        toolbar.goBackButton.setToolTipText("Go Back a Cell");
        toolbar.goBackButton.setEnabled(false);
        toolbar.goBackButton.addMouseListener(
            new MouseListener() {
                public void mouseClicked(MouseEvent e) {}
                public void mouseEntered(MouseEvent e) {}
                public void mouseExited(MouseEvent e) {}
                public void mousePressed(MouseEvent e) {}
                public void mouseReleased(MouseEvent e) {
                    AbstractButton b = (AbstractButton) e.getSource();
                    if(ClickZoomWireListener.isRightMouse(e) && b.contains(e.getX(), e.getY()))
                        showHistoryPopup(e);
                }
            }
        );
        toolbar.add(toolbar.goBackButton);

        toolbar.goForwardButton = new JButton(Resources.getResource(toolbar.getClass(), "ButtonGoForward.gif"));
        toolbar.goForwardButton.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { goForwardButtonCommand(); } });
        toolbar.goForwardButton.setToolTipText("Go Forward a Cell");
        toolbar.goForwardButton.setEnabled(false);
        toolbar.goForwardButton.addMouseListener(
            new MouseListener() {
                public void mouseClicked(MouseEvent e) {}
                public void mouseEntered(MouseEvent e) {}
                public void mouseExited(MouseEvent e) {}
                public void mousePressed(MouseEvent e) {}
                public void mouseReleased(MouseEvent e) {
                    AbstractButton b = (AbstractButton) e.getSource();
                    if(ClickZoomWireListener.isRightMouse(e) && b.contains(e.getX(), e.getY()))
                        showHistoryPopup(e);
                }
            }
        );
        toolbar.add(toolbar.goForwardButton);

		// a separator
		toolbar.addSeparator();

		// the "Expanded" button
		ToolBarButton expandButton = ToolBarButton.newInstance("Expand Cell Instances",
            Resources.getResource(toolbar.getClass(), "ButtonExpand.gif"));
		expandButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.expandOneLevelDownCommand(); } });
		expandButton.setToolTipText("Expand Cell Instances");
        expandButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
		toolbar.add(expandButton);

		// the "Unexpanded" button
		ToolBarButton unExpandButton = ToolBarButton.newInstance("Unexpand Cell Instances",
            Resources.getResource(toolbar.getClass(), "ButtonUnexpand.gif"));
		unExpandButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { CircuitChanges.unexpandOneLevelUpCommand(); } });
		unExpandButton.setToolTipText("Unexpand Cell Instances");
        unExpandButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
		toolbar.add(unExpandButton);

		// return the toolbar
		return toolbar;
	}

    // ------------------------------------------------------------------------------------

	public static Cursor readCursor(String cursorName, int hotX, int hotY)
	{
		ImageIcon imageIcon = Resources.getResource(ToolBar.class, cursorName);
		Image image = imageIcon.getImage();
		int width = image.getWidth(null);
		int height = image.getHeight(null);
		Dimension bestSize = Toolkit.getDefaultToolkit().getBestCursorSize(width, height);
		int bestWidth = (int)bestSize.getWidth();
		int bestHeight = (int)bestSize.getHeight();
		if (bestWidth != 0 && bestHeight != 0)
		{
			if (bestWidth != width || bestHeight != height)
			{
				if (bestWidth > width && bestHeight > height)
				{
					// want a larger cursor, so just pad this one
					Image newImage = new BufferedImage(bestWidth, bestHeight, BufferedImage.TYPE_INT_ARGB);
					Graphics g = newImage.getGraphics();
					g.drawImage(image, (bestWidth-width)/2, (bestHeight-height)/2, null);
					image = newImage;
					hotX += (bestWidth-width)/2;
					hotY += (bestHeight-height)/2;
				} else
				{
					// want a smaller cursor, so scale this one
					image = image.getScaledInstance(bestWidth, bestHeight, 0);
					hotX = hotX * bestWidth / width;
					hotY = hotY * bestHeight / height;
				}
			}
		}
		Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(hotX, hotY), cursorName);
		return cursor;
	}

    public static void clickZoomWireCommand()
    {
    	checkLeavingOutlineMode();
        WindowFrame.setListener(ClickZoomWireListener.theOne);
        TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        curMode = CursorMode.CLICKZOOMWIRE;
    }

    /**
     * Method called to toggle the state of the "select special"
     * button.
     */
    public static void toggleSelectSpecialCommand(ActionEvent e)
    {
        // get new state of special select to set icon
        AbstractButton b = (AbstractButton)e.getSource();
        if (b.isSelected()) {
            ToolBarButton.setIconForButton(specialSelectName, selectSpecialIconOn);
        } else {
            ToolBarButton.setIconForButton(specialSelectName, selectSpecialIconOff);
        }
    }
    
	/**
	 * Method called when the "pan" button is pressed.
	 */
	public static void panCommand()
	{
        if (WindowFrame.getListener() == ZoomAndPanListener.theOne && curMode == CursorMode.PAN) {
            // switch back to click zoom wire listener
            setCursorMode(CursorMode.CLICKZOOMWIRE);
            return;
        }
		WindowFrame.setListener(ZoomAndPanListener.theOne);
		//makeCursors();
		TopLevel.setCurrentCursor(panCursor);
		curMode = CursorMode.PAN;
	}

	/**
	 * Method called when the "zoom" button is pressed.
	 */
	public static void zoomCommand()
	{
        if (WindowFrame.getListener() == ZoomAndPanListener.theOne && curMode == CursorMode.ZOOM) {
            // switch back to click zoom wire listener
            setCursorMode(CursorMode.CLICKZOOMWIRE);
            return;
        }
        checkLeavingOutlineMode();
		WindowFrame.setListener(ZoomAndPanListener.theOne);
		TopLevel.setCurrentCursor(zoomCursor);
		curMode = CursorMode.ZOOM;
	}

	/**
	 * Method called when the "outline edit" button is pressed.
	 */
	public static void outlineEditCommand()
	{
        if (WindowFrame.getListener() == OutlineListener.theOne) {
            // switch back to click zoom wire listener
            setCursorMode(CursorMode.CLICKZOOMWIRE);
            return;
        }

//        Cell cell = WindowFrame.needCurCell();
        EditWindow wnd = EditWindow.needCurrent();
        if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

        CursorMode oldMode = curMode;
		NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
		if (ni == null)
		{
			if (oldMode == CursorMode.OUTLINE) clickZoomWireCommand(); else
                setCursorMode(oldMode);
			return;
		}
		NodeProto np = ni.getProto();
		if (!(np instanceof PrimitiveNode && ((PrimitiveNode)np).isHoldsOutline()))
		{
			System.out.println("Sorry, " + np + " does not hold outline information");
            if (oldMode == CursorMode.OUTLINE) clickZoomWireCommand(); else
                setCursorMode(oldMode);
			return;
		}

		if (WindowFrame.getListener() != OutlineListener.theOne)
			OutlineListener.theOne.setNode(ni);
		WindowFrame.setListener(OutlineListener.theOne);
		TopLevel.setCurrentCursor(outlineCursor);
		curMode = CursorMode.OUTLINE;
	}

    private static void checkLeavingOutlineMode()
    {
    	// if exiting outline-edit mode, turn off special display
        if (WindowFrame.getListener() == OutlineListener.theOne && curMode == CursorMode.OUTLINE)
    	{
            EditWindow wnd = EditWindow.needCurrent();
            if (wnd != null)
            {
            	Highlighter highlighter = wnd.getHighlighter();
            	NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
        		if (ni != null)
        		{
        			Highlight high = highlighter.getOneHighlight();
        			if (high != null)
        			{
        				high.setPoint(-1);
        				wnd.repaint();
        			}
        		}
            }
    	}
    }

	/**
	 * Method called when the "measure" button is pressed.
	 */
	public static void measureCommand()
	{
        if (WindowFrame.getListener() == MeasureListener.theOne) {
            // switch back to click zoom wire listener
            setCursorMode(CursorMode.CLICKZOOMWIRE);
            return;
        }
        checkLeavingOutlineMode();
        MeasureListener.theOne.reset();
		WindowFrame.setListener(MeasureListener.theOne);
		TopLevel.setCurrentCursor(measureCursor);
		curMode = CursorMode.MEASURE;
	}

    /**
     * Set cursor mode
     * @param mode the CursorMode corresponding to the desired mode
     */
    public static void setCursorMode(CursorMode mode)
    {
        if (mode == CursorMode.CLICKZOOMWIRE) ToolBarButton.doClick(cursorClickZoomWireName);
        else if (mode == CursorMode.PAN) ToolBarButton.doClick(cursorPanName);
        else if (mode == CursorMode.ZOOM) ToolBarButton.doClick(cursorZoomName);
		else if (mode == CursorMode.OUTLINE) ToolBarButton.doClick(cursorOutlineName);
		else if (mode == CursorMode.MEASURE) ToolBarButton.doClick(cursorMeasureName);
    }

	/**
	 * Method to tell which cursor mode is in effect.
	 * @return the current mode (select, pan, zoom, outline, measure).
	 */
	public static CursorMode getCursorMode() { return curMode; }

	/**
	 * Method called when the "full arrow distance" button is pressed.
     * Method called when the "half arrow distance" button is pressed.
     * Method called when the "quarter arrow distance" button is pressed.
	 */
	public static void arrowDistanceCommand(ArrowDistance arrow)
	{
        curArrowDistance = arrow;
        double dist = -1; // no valid value as default
        if (arrow == ArrowDistance.FULL)
            dist = 1;
        else if (arrow == ArrowDistance.HALF)
            dist = 0.5;
        else if (arrow == ArrowDistance.QUARTER)
            dist = 0.25;
        EditMenu.setGridAligment(dist);
        User.setAlignmentToGrid(dist);
	}

	/**
	 * Method called when the "select objects" button is pressed.
	 */
	public static void selectObjectsCommand()
	{
		curSelectMode = SelectMode.OBJECTS;
	}

	/**
	 * Method called when the "select area" button is pressed.
	 */
	public static void selectAreaCommand()
	{
		curSelectMode = SelectMode.AREA;
	}

	/**
	 * Method to tell what selection mode is in effect.
	 * @return the current selection mode (objects or area).
	 */
	public static SelectMode getSelectMode() { return curSelectMode; }

    /**
     * Returns state of "select special" button
     * @return true if select special button selected, false otherwise
     */
    public static boolean getSelectSpecial() { return ToolBarButton.getButtonState(specialSelectName); }

    /**
     * Go back in the cell history
     */
    public static void goBackButtonCommand() {
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf != null) wf.getContent().cellHistoryGoBack();
    }

    /**
     * Go forward in the cell history
     */
    public static void goForwardButtonCommand() {
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf != null) wf.getContent().cellHistoryGoForward();
    }

    public static void showHistoryPopup(MouseEvent e) {
        EditWindow wnd = EditWindow.getCurrent();
        if (wnd == null) return;
        EditWindow.CellHistoryState history = wnd.getCellHistory();

        JPopupMenu popup = new JPopupMenu();
        JMenuItem m;

        HashMap<Cell,Cell> listed = new HashMap<Cell,Cell>();
        //for (int i=0; i<history.getHistory().size(); i++) {
        for (int i=history.getHistory().size()-1; i > -1; i--) {
            EditWindow.CellHistory entry = (EditWindow.CellHistory)history.getHistory().get(i);
            Cell cell = entry.getCell();
            // skip if already shown such a cell
			if (cell == null) continue;
            if (listed.get(cell) != null) continue;
            listed.put(cell, cell);

            boolean shown = (i == history.getLocation());
            m = new JMenuItem(cell.noLibDescribe() + (shown? "  (shown)" : ""));
            m.addActionListener(new HistoryPopupAction(wnd, i));
            popup.add(m);
        }
        Component invoker = e.getComponent();
        if (invoker != null) {
            popup.setInvoker(invoker);
            Point2D loc = invoker.getLocationOnScreen();
            popup.setLocation((int)loc.getX() + invoker.getWidth()/2, (int)loc.getY() + invoker.getHeight()/2);
        }
        popup.setVisible(true);

    }

    private static class HistoryPopupAction implements ActionListener {
        private final EditWindow wnd;
        private final int historyLocation;
        private HistoryPopupAction(EditWindow wnd, int loc) {
            this.wnd = wnd;
            this.historyLocation = loc;
        }
        public void actionPerformed(ActionEvent e) {
            wnd.setCellByHistory(historyLocation);
        }
    }

	public void setSaveLibraryButton(boolean value)
	{
		saveLibraryButton.setEnabled(value);
	}
    // ----------------------------------------------------------------------------

    /**
     * Call when done with this toolBar to release its resources
     */
    public void finished()
    {
        // find ToolBarButtons
        Component [] components = getComponents();
        for (int i=0; i<components.length; i++) {
            Component component = components[i];
            if (component instanceof ToolBarButton) {
                ToolBarButton b = (ToolBarButton)component;
                b.finished();
            }
        }
    }

    /**
     * Listen for property change events.  Currently this supports
     * events generated by an EditWindow when the ability to traverse
     * backwards or forwards through it's cell history changes.
     * <p>It also now supports when the Undo/Redo buttons change enabled state.
     * @param evt The property change event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        String name = evt.getPropertyName();
        boolean enabled = ((Boolean)evt.getNewValue()).booleanValue();
        if (name.equals(EditWindow.propGoBackEnabled)) {
            goBackButton.setEnabled(enabled);
        }
        else if (name.equals(EditWindow.propGoForwardEnabled)) {
            goForwardButton.setEnabled(enabled);
        }
        else if (name.equals(Undo.propUndoEnabled)) {
            undoButton.setEnabled(enabled);
        }
        else if (name.equals(Undo.propRedoEnabled)) {
            redoButton.setEnabled(enabled);
        }
    }

    /**
     * Listen for InternalFrame activation events.  Currently this supports
     * when a JInternalFrame gets raised to front, we associate the forward
     * and back history buttons with the EditWindow in that frame
     * @param e the event.
     */
    public void internalFrameActivated(InternalFrameEvent e) {
        JInternalFrame source = e.getInternalFrame();
        if (source instanceof JInternalFrame) {
            // associate go back and forward states with that frame's window
            WindowFrame useFrame = null;
            for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
                WindowFrame frame = (WindowFrame)it.next();
                if (frame.generatedEvent(e)) {
                    useFrame = frame;
                    break;
                }
            }
            if (useFrame == null) return;
			WindowContent content = useFrame.getContent();
            if (content == null) return;
            goBackButton.setEnabled(content.cellHistoryCanGoBack());
            goForwardButton.setEnabled(content.cellHistoryCanGoForward());
        }
    }

    public void internalFrameClosed(InternalFrameEvent e) {}

    public void internalFrameClosing(InternalFrameEvent e) {}

    public void internalFrameDeactivated(InternalFrameEvent e) {}

    public void internalFrameDeiconified(InternalFrameEvent e) {}

    public void internalFrameIconified(InternalFrameEvent e) {}

    public void internalFrameOpened(InternalFrameEvent e) {}

    public void setEnabled(String name, boolean flag)
    {
	    if (name.equals(SaveLibraryName))
		    saveLibraryButton.setEnabled(flag);
    }
}
