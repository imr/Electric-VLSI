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

import com.sun.electric.tool.user.ui.Button;
import com.sun.electric.tool.user.UserMenuCommands;

import javax.swing.JToolBar;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.Insets;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.Point;
import java.awt.Dimension;


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

	private static JToggleButton selectButton, wireButton, selectSpecialButton, panButton, zoomButton;
	private static JToggleButton fullButton, halfButton, quarterButton;
	private static JToggleButton objectsButton, areaButton;
	private static ButtonGroup modeGroup, arrowGroup, selectGroup;
	private static CursorMode curMode = CursorMode.SELECT;
	private static ArrowDistance curArrowDistance = ArrowDistance.FULL;
	private static SelectMode curSelectMode = SelectMode.OBJECTS;
	private static Cursor zoomCursor = null;
	private static Cursor panCursor = null;
	private static Cursor specialSelectCursor = null;
	private static Cursor wiringCursor = null;

	private ToolBar() {}

	/**
	 * Routine to create the toolbar.
	 */
	public static ToolBar createToolBar()
	{
		// create the toolbar
		ToolBar toolbar = new ToolBar();
		toolbar.setFloatable(true);
		toolbar.setRollover(true);

		// the "Open file" button
		JButton openButton = Button.newInstance(new ImageIcon(toolbar.getClass().getResource("ButtonOpen.gif")));
		openButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { UserMenuCommands.openLibraryCommand(); } });
		openButton.setToolTipText("Open");
		toolbar.add(openButton);

		// a separator
		toolbar.addSeparator();

		// the "Select mode" button
		modeGroup = new ButtonGroup();
		selectButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonSelect.gif")));
		selectButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectCommand(); } });
		selectButton.setToolTipText("Select");
		selectButton.setSelected(true);
		toolbar.add(selectButton);
		modeGroup.add(selectButton);

		// the "Wiring" button
		wireButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonWiring.gif")));
		wireButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { wiringCommand(); } });
		wireButton.setToolTipText("Wiring");
		toolbar.add(wireButton);
		modeGroup.add(wireButton);

		// the "Special select mode" button
		selectSpecialButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonSelectSpecial.gif")));
		selectSpecialButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectSpecialCommand(); } });
		selectSpecialButton.setToolTipText("Special Select");
		toolbar.add(selectSpecialButton);
		modeGroup.add(selectSpecialButton);

		// the "Pan mode" button
		panButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonPan.gif")));
		panButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { panCommand(); } });
		panButton.setToolTipText("Pan");
		toolbar.add(panButton);
		modeGroup.add(panButton);

		// the "Zoom mode" button
		zoomButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonZoom.gif")));
		zoomButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { zoomCommand(); } });
		zoomButton.setToolTipText("Zoom");
		toolbar.add(zoomButton);
		modeGroup.add(zoomButton);

		// a separator
		toolbar.addSeparator();

		// the "Full arrow distance" button
		arrowGroup = new ButtonGroup();
		fullButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonFull.gif")));
		fullButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { fullArrowDistanceCommand(); } });
		fullButton.setToolTipText("Full motion");
		fullButton.setSelected(true);
		toolbar.add(fullButton);
		arrowGroup.add(fullButton);

		// the "Half arrow distance" button
		halfButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonHalf.gif")));
		halfButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { halfArrowDistanceCommand(); } });
		halfButton.setToolTipText("Half motion");
		toolbar.add(halfButton);
		arrowGroup.add(halfButton);

		// the "Quarter arrow distance" button
		quarterButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonQuarter.gif")));
		quarterButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { quarterArrowDistanceCommand(); } });
		quarterButton.setToolTipText("Quarter motion");
		toolbar.add(quarterButton);
		arrowGroup.add(quarterButton);

		// a separator
		toolbar.addSeparator();

		// the "Select Objects" button
		selectGroup = new ButtonGroup();
		objectsButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonObjects.gif")));
		objectsButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectObjectsCommand(); } });
		objectsButton.setToolTipText("Select Objects");
		objectsButton.setSelected(true);
		toolbar.add(objectsButton);
		selectGroup.add(objectsButton);

		// the "Select Area" button
		areaButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonArea.gif")));
		areaButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectAreaCommand(); } });
		areaButton.setToolTipText("Select Area");
		toolbar.add(areaButton);
		selectGroup.add(areaButton);

		// a separator
		toolbar.addSeparator();

//		// a test button
//		Button testButton = Button.newInstance(new ImageIcon(toolbar.getClass().getResource("ButtonTest.gif")));
//		testButton.setToolTipText("test");
//
//		// set an area for popup menu to be triggered within a button
//		Insets insets = new Insets(22,22,32,32);
//		JPopupMenu popup = new JPopupMenu();
//		JMenuItem testItem = new JMenuItem("test 1");
//		popup.add(testItem);
//		testItem = new JMenuItem("test 2");
//		popup.add(testItem);
//		testButton.addPopupMenu(popup, insets);
//		toolbar.add(testButton);

		// return the toolbar
		return toolbar;
	}

	/**
	 * Routine called when the "select" button is pressed.
	 */
	public static void selectCommand()
	{
		EditWindow.setListener(ClickAndDragListener.theOne);
		TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		curMode = CursorMode.SELECT;
		selectButton.setSelected(true);
		UserMenuCommands.cursorSelect.setSelected(true);
	}

	/**
	 * Routine called when the "wiring" button is pressed.
	 */
	public static void wiringCommand()
	{
		EditWindow.setListener(WiringListener.theOne);
		if (wiringCursor == null) wiringCursor = readCursor("CursorWiring.gif", 8, 8);
		TopLevel.setCurrentCursor(wiringCursor);
		curMode = CursorMode.WIRE;
		wireButton.setSelected(true);
		UserMenuCommands.cursorWiring.setSelected(true);
	}

	/**
	 * Routine called when the "select special" button is pressed.
	 */
	public static void selectSpecialCommand()
	{
		EditWindow.setListener(ClickAndDragListener.theOne);
		if (specialSelectCursor == null) specialSelectCursor = readCursor("CursorSelectSpecial.gif", 8, 9);
		TopLevel.setCurrentCursor(specialSelectCursor);
		curMode = CursorMode.SELECTSPECIAL;
		selectSpecialButton.setSelected(true);
		UserMenuCommands.cursorSelectSpecial.setSelected(true);
	}

	/**
	 * Routine called when the "pan" button is pressed.
	 */
	public static void panCommand()
	{
		EditWindow.setListener(ZoomAndPanListener.theOne);
		if (panCursor == null) panCursor = readCursor("CursorPan.gif", 16, 16);
		TopLevel.setCurrentCursor(panCursor);
		curMode = CursorMode.PAN;
		panButton.setSelected(true);
		UserMenuCommands.cursorPan.setSelected(true);
	}

	/**
	 * Routine called when the "zoom" button is pressed.
	 */
	public static void zoomCommand()
	{
		EditWindow.setListener(ZoomAndPanListener.theOne);
		if (zoomCursor == null) zoomCursor = readCursor("CursorZoom.gif", 13, 13);
		TopLevel.setCurrentCursor(zoomCursor);
		curMode = CursorMode.ZOOM;
		zoomButton.setSelected(true);
		UserMenuCommands.cursorZoom.setSelected(true);
	}

	/**
	 * Routine to tell which cursor mode is in effect.
	 * @return the current mode (select, special-select, pan, or zoom).
	 */
	public static CursorMode getCursorMode() { return curMode; }

	/**
	 * Routine called when the "full arrow distance" button is pressed.
	 */
	public static void fullArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.FULL;
		fullButton.setSelected(true);
		UserMenuCommands.moveFull.setSelected(true);
	}

	/**
	 * Routine called when the "half arrow distance" button is pressed.
	 */
	public static void halfArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.HALF;
		halfButton.setSelected(true);
		UserMenuCommands.moveHalf.setSelected(true);
	}

	/**
	 * Routine called when the "quarter arrow distance" button is pressed.
	 */
	public static void quarterArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.QUARTER;
		quarterButton.setSelected(true);
		UserMenuCommands.moveQuarter.setSelected(true);
	}

	/**
	 * Routine to tell what arrow distance is in effect.
	 * @return the current arrow distance (1.0, 0.5, or 0.25).
	 */
	public static double getArrowDistance() { return curArrowDistance.getDistance(); }

	/**
	 * Routine called when the "select objects" button is pressed.
	 */
	public static void selectObjectsCommand()
	{
		curSelectMode = SelectMode.OBJECTS;
		objectsButton.setSelected(true);
		UserMenuCommands.selectObjects.setSelected(true);
	}

	/**
	 * Routine called when the "select area" button is pressed.
	 */
	public static void selectAreaCommand()
	{
		curSelectMode = SelectMode.AREA;
		areaButton.setSelected(true);
		UserMenuCommands.selectArea.setSelected(true);
	}

	/**
	 * Routine to tell what selection mode is in effect.
	 * @return the current selection mode (objects or area).
	 */
	public static SelectMode getSelectMode() { return curSelectMode; }

	private static Cursor readCursor(String cursorName, int hotX, int hotY)
	{
		ImageIcon imageIcon = new ImageIcon(ToolBar.class.getResource(cursorName));
		Image image = imageIcon.getImage();
//		int width = image.getWidth(null);
//		int height = image.getHeight(null);
//		Dimension bestSize = Toolkit.getDefaultToolkit().getBestCursorSize(width, height);
//		width = (int)bestSize.getWidth();
//		height = (int)bestSize.getHeight();
//		image = image.getScaledInstance(width, height, 0);
		Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(image, new Point(hotX, hotY), cursorName);
		return cursor;
	}

}
