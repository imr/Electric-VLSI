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

import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.user.MenuCommands;
import com.sun.electric.tool.user.Highlight;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import javax.swing.JToolBar;
import javax.swing.ImageIcon;
import javax.swing.ButtonGroup;
import javax.swing.AbstractButton;


/**
 * This class manages the Electric toolbar.
 */
public class ToolBar extends JToolBar
{

    /**
	 * Mode is a typesafe enum class that describes the current editing mode (select, zoom, etc).
	 */
	public static class CursorMode
	{
		private String name;

		private CursorMode(String name) { this.name = name; }

		public String toString() { return "CursorMode="+name; }
        
		/** Describes Selection mode (click and drag). */		public static final CursorMode SELECT = new CursorMode("select");
		/** Describes wiring mode (creating arcs). */			public static final CursorMode WIRE = new CursorMode("wire");
		/** Describes Selection mode (click and drag). */		public static final CursorMode SELECTSPECIAL = new CursorMode("select");
		/** Describes Panning mode (move window contents). */	public static final CursorMode PAN = new CursorMode("pan");
		/** Describes Zoom mode (scale window contents). */		public static final CursorMode ZOOM = new CursorMode("zoom");
        /** Describes ClickZoomWire mode (does everything). */  public static final CursorMode CLICKZOOMWIRE = new CursorMode("clickzoomwire");
		/** Describes Outline edit mode. */						public static final CursorMode OUTLINE = new CursorMode("outline");
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

	private static CursorMode curMode = CursorMode.SELECT;
	private static ArrowDistance curArrowDistance = ArrowDistance.FULL;
	private static SelectMode curSelectMode = SelectMode.OBJECTS;
    private static Cursor zoomCursor = readCursor("CursorZoom.gif", 6, 6);
	private static Cursor panCursor = readCursor("CursorPan.gif", 8, 8);
	private static Cursor specialSelectCursor = readCursor("CursorSelectSpecial.gif", 0, 1);
	private static Cursor wiringCursor = readCursor("CursorWiring.gif", 0, 0);
	private static Cursor outlineCursor = readCursor("CursorOutline.gif", 0, 0);
	private static ToolBar toolbar = null;

    public static final ImageIcon selectSpecialIconOn = new ImageIcon(ToolBar.class.getResource("ButtonSelectSpecialOn.gif"));
    public static final ImageIcon selectSpecialIconOff = new ImageIcon(ToolBar.class.getResource("ButtonSelectSpecialOff.gif"));
    
	private ToolBar() {}

	/**
	 * Method to create the toolbar.
	 */
	public static ToolBar createToolBar()
	{
		// create the toolbar
		toolbar = new ToolBar();
		toolbar.setFloatable(true);
		toolbar.setRollover(true);
        
        ToolBarButton clickZoomWireButton, selectButton, wireButton, panButton, zoomButton, outlineButton;
        ToolBarButton fullButton, halfButton, quarterButton;
        ToolBarButton objectsButton, areaButton;
        ToolBarButton selectSpecialButton;
        ButtonGroup modeGroup, arrowGroup, selectGroup;
        
		// the "Select mode" button
		modeGroup = new ButtonGroup();

        clickZoomWireButton = ToolBarButton.newInstance(MenuCommands.cursorClickZoomWireName,
            new ImageIcon(toolbar.getClass().getResource("ButtonClickZoomWire.gif")));
        clickZoomWireButton.addActionListener(
            new ActionListener() { public void actionPerformed(ActionEvent e) { clickZoomWireCommand(); } });
        clickZoomWireButton.setToolTipText("ClickZoomWire");
        clickZoomWireButton.setSelected(true);
        toolbar.add(clickZoomWireButton);
        modeGroup.add(clickZoomWireButton);

		selectButton = ToolBarButton.newInstance(MenuCommands.cursorSelectName,
            new ImageIcon(toolbar.getClass().getResource("ButtonSelect.gif")));
		selectButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectCommand(); } });
		selectButton.setToolTipText("Select");
		toolbar.add(selectButton);
		modeGroup.add(selectButton);

		// the "Wiring" button
		wireButton = ToolBarButton.newInstance(MenuCommands.cursorWiringName, 
            new ImageIcon(toolbar.getClass().getResource("ButtonWiring.gif")));
		wireButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { wiringCommand(); } });
		wireButton.setToolTipText("Wiring");
		toolbar.add(wireButton);
		modeGroup.add(wireButton);

		// the "Pan mode" button
		panButton = ToolBarButton.newInstance(MenuCommands.cursorPanName,
            new ImageIcon(toolbar.getClass().getResource("ButtonPan.gif")));
		panButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { panCommand(); } });
		panButton.setToolTipText("Pan");
		toolbar.add(panButton);
		modeGroup.add(panButton);

		// the "Zoom mode" button
		zoomButton = ToolBarButton.newInstance(MenuCommands.cursorZoomName, 
            new ImageIcon(toolbar.getClass().getResource("ButtonZoom.gif")));
		zoomButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { zoomCommand(); } });
		zoomButton.setToolTipText("Zoom");
		toolbar.add(zoomButton);
		modeGroup.add(zoomButton);

		// the "Outline edit mode" button
		outlineButton = ToolBarButton.newInstance(MenuCommands.cursorOutlineName, 
            new ImageIcon(toolbar.getClass().getResource("ButtonOutline.gif")));
		outlineButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { outlineEditCommand(); } });
		outlineButton.setToolTipText("Outline Edit");
		toolbar.add(outlineButton);
		modeGroup.add(outlineButton);

		// a separator
		toolbar.addSeparator();

		// the "Full arrow distance" button
		arrowGroup = new ButtonGroup();
		fullButton = ToolBarButton.newInstance(MenuCommands.moveFullName, 
            new ImageIcon(toolbar.getClass().getResource("ButtonFull.gif")));
		fullButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { fullArrowDistanceCommand(); } });
		fullButton.setToolTipText("Full motion");
		fullButton.setSelected(true);
		toolbar.add(fullButton);
		arrowGroup.add(fullButton);

		// the "Half arrow distance" button
        halfButton = ToolBarButton.newInstance(MenuCommands.moveHalfName, 
            new ImageIcon(toolbar.getClass().getResource("ButtonHalf.gif")));
		halfButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { halfArrowDistanceCommand(); } });
		halfButton.setToolTipText("Half motion");
		toolbar.add(halfButton);
		arrowGroup.add(halfButton);

		// the "Quarter arrow distance" button
		quarterButton = ToolBarButton.newInstance(MenuCommands.moveQuarterName, 
            new ImageIcon(toolbar.getClass().getResource("ButtonQuarter.gif")));
		quarterButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { quarterArrowDistanceCommand(); } });
		quarterButton.setToolTipText("Quarter motion");
		toolbar.add(quarterButton);
		arrowGroup.add(quarterButton);

		// a separator
		toolbar.addSeparator();

        // the "Select Objects" button
		selectGroup = new ButtonGroup();
		objectsButton = ToolBarButton.newInstance(MenuCommands.selectObjectsName, 
            new ImageIcon(toolbar.getClass().getResource("ButtonObjects.gif")));
		objectsButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectObjectsCommand(); } });
		objectsButton.setToolTipText("Select Objects");
		objectsButton.setSelected(true);
		toolbar.add(objectsButton);
		selectGroup.add(objectsButton);
        
        // the "Select Area" button
		areaButton = ToolBarButton.newInstance(MenuCommands.selectAreaName, 
            new ImageIcon(toolbar.getClass().getResource("ButtonArea.gif")));
		areaButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectAreaCommand(); } });
		areaButton.setToolTipText("Select Area");
		toolbar.add(areaButton);
		selectGroup.add(areaButton);
        
		// the "Special select mode" button
        // this is a true toggle button, default is no special select
		selectSpecialButton = ToolBarButton.newInstance(MenuCommands.specialSelectName, selectSpecialIconOff);
		selectSpecialButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { toggleSelectSpecialCommand(e); } });
		selectSpecialButton.setToolTipText("Toggle Special Select");
		toolbar.add(selectSpecialButton);
        
		// a separator
		toolbar.addSeparator();

		// the "Options" buttons
		ToolBarButton editOptionButton = ToolBarButton.newInstance("Edit Options", 
            new ImageIcon(toolbar.getClass().getResource("ButtonOptionEdit.gif")));
		editOptionButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.editOptionsCommand(); } });
		editOptionButton.setToolTipText("Edit Options");
        editOptionButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
        toolbar.add(editOptionButton);


		ToolBarButton toolOptionButton = ToolBarButton.newInstance("Tool Options", 
            new ImageIcon(toolbar.getClass().getResource("ButtonOptionTool.gif")));
		toolOptionButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.toolOptionsCommand(); } });
		toolOptionButton.setToolTipText("Tool Options");
        toolOptionButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
		toolbar.add(toolOptionButton);

		ToolBarButton ioOptionButton = ToolBarButton.newInstance("I/O Options", 
            new ImageIcon(toolbar.getClass().getResource("ButtonOptionIO.gif")));
		ioOptionButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.ioOptionsCommand(); } });
		ioOptionButton.setToolTipText("I/O Options");
        ioOptionButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
		toolbar.add(ioOptionButton);

		// a separator
		toolbar.addSeparator();

		// the "Expanded" button
		ToolBarButton expandButton = ToolBarButton.newInstance("Expand Cell Instances", 
            new ImageIcon(toolbar.getClass().getResource("ButtonExpand.gif")));
		expandButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.expandOneLevelDownCommand(); } });
		expandButton.setToolTipText("Expand Cell Instances");
        expandButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
		toolbar.add(expandButton);

		// the "Unexpanded" button
		ToolBarButton unExpandButton = ToolBarButton.newInstance("Unexpand Cell Instances", 
            new ImageIcon(toolbar.getClass().getResource("ButtonUnexpand.gif")));
		unExpandButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.unexpandOneLevelUpCommand(); } });
		unExpandButton.setToolTipText("Unexpand Cell Instances");
        unExpandButton.setModel(new javax.swing.DefaultButtonModel());  // this de-highlights the button after it is released
		toolbar.add(unExpandButton);

		// return the toolbar
		return toolbar;
	}

	private static void makeCursors()
	{
		if (wiringCursor == null) wiringCursor = readCursor("CursorWiring.gif", 0, 0);
		if (specialSelectCursor == null) specialSelectCursor = readCursor("CursorSelectSpecial.gif", 0, 1);
		if (panCursor == null) panCursor = readCursor("CursorPan.gif", 8, 8);
		if (zoomCursor == null) zoomCursor = readCursor("CursorZoom.gif", 6, 6);
		if (outlineCursor == null) outlineCursor = readCursor("CursorOutline.gif", 0, 0);
	}

	private static Cursor readCursor(String cursorName, int hotX, int hotY)
	{
		ImageIcon imageIcon = new ImageIcon(ToolBar.class.getResource(cursorName));
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
        EditWindow.setListener(ClickZoomWireListener.theOne);
        TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        curMode = CursorMode.CLICKZOOMWIRE;
    }

	/**
	 * Method called when the "select" button is pressed.
	 */
	public static void selectCommand()
	{
		EditWindow.setListener(ClickAndDragListener.theOne);
		TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		curMode = CursorMode.SELECT;
	}

	/**
	 * Method called when the "wiring" button is pressed.
	 */
	public static void wiringCommand()
	{
		EditWindow.setListener(WiringListener.theOne);
		//makeCursors();
		TopLevel.setCurrentCursor(wiringCursor);
		curMode = CursorMode.WIRE;
	}

	/**
	 * Method called when the "select special" button is pressed.
     * Depricated - replaced by toggleSpecialSelectCommand.
	 */
	public static void selectSpecialCommand()
	{
		EditWindow.setListener(ClickAndDragListener.theOne);
		//makeCursors();
		TopLevel.setCurrentCursor(specialSelectCursor);
		curMode = CursorMode.SELECTSPECIAL;
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
            ToolBarButton.setIconForButton(MenuCommands.specialSelectName, selectSpecialIconOn);
        } else {
            ToolBarButton.setIconForButton(MenuCommands.specialSelectName, selectSpecialIconOff);
        }
    }
    
	/**
	 * Method called when the "pan" button is pressed.
	 */
	public static void panCommand()
	{
		EditWindow.setListener(ZoomAndPanListener.theOne);
		//makeCursors();
		TopLevel.setCurrentCursor(panCursor);
		curMode = CursorMode.PAN;
	}

	/**
	 * Method called when the "zoom" button is pressed.
	 */
	public static void zoomCommand()
	{
		EditWindow.setListener(ZoomAndPanListener.theOne);
		TopLevel.setCurrentCursor(zoomCursor);
		curMode = CursorMode.ZOOM;
	}

	/**
	 * Method called when the "outline edit" button is pressed.
	 */
	public static void outlineEditCommand()
	{
        CursorMode oldMode = curMode;
		NodeInst ni = (NodeInst)Highlight.getOneElectricObject(NodeInst.class);
		if (ni == null)
		{
			System.out.println("Must first select a node with outline capabilities");
			if (oldMode == CursorMode.OUTLINE) selectCommand(); else
                setCursorMode(oldMode);
			return;
		}
		if (!ni.getProto().isHoldsOutline())
		{
			System.out.println("Cannot edit outline information on " + ni.getProto().describe() + " nodes");
            if (oldMode == CursorMode.OUTLINE) selectCommand(); else
                setCursorMode(oldMode);
			return;
		}

		if (EditWindow.getListener() != OutlineListener.theOne)
			OutlineListener.theOne.setNode(ni);
		EditWindow.setListener(OutlineListener.theOne);
		TopLevel.setCurrentCursor(outlineCursor);
		curMode = CursorMode.OUTLINE;
	}

    /**
     * Set cursor mode
     * @param mode the CursorMode corresponding to the desired mode
     */
    public static void setCursorMode(CursorMode mode)
    {
        if (mode == CursorMode.CLICKZOOMWIRE) ToolBarButton.doClick(MenuCommands.cursorClickZoomWireName);
        else if (mode == CursorMode.SELECT) ToolBarButton.doClick(MenuCommands.cursorSelectName);
        else if (mode == CursorMode.WIRE) ToolBarButton.doClick(MenuCommands.cursorWiringName);
        else if (mode == CursorMode.PAN) ToolBarButton.doClick(MenuCommands.cursorPanName);
        else if (mode == CursorMode.ZOOM) ToolBarButton.doClick(MenuCommands.cursorZoomName);
        else if (mode == CursorMode.OUTLINE) ToolBarButton.doClick(MenuCommands.cursorOutlineName);
    }

	/**
	 * Method to tell which cursor mode is in effect.
	 * @return the current mode (select, special-select, pan, zoom, or outline).
	 */
	public static CursorMode getCursorMode() { return curMode; }

	/**
	 * Method called when the "full arrow distance" button is pressed.
	 */
	public static void fullArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.FULL;
	}

	/**
	 * Method called when the "half arrow distance" button is pressed.
	 */
	public static void halfArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.HALF;
	}

	/**
	 * Method called when the "quarter arrow distance" button is pressed.
	 */
	public static void quarterArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.QUARTER;
	}

	/**
	 * Method to tell what arrow distance is in effect.
	 * @return the current arrow distance (1.0, 0.5, or 0.25).
	 */
	public static double getArrowDistance() { return curArrowDistance.getDistance(); }

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
    public static boolean getSelectSpecial() { return ToolBarButton.getButtonState(MenuCommands.specialSelectName); }
}
