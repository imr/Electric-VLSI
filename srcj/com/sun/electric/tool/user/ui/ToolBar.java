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
import com.sun.electric.tool.user.ui.Button;

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
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.ButtonGroup;
import javax.swing.AbstractAction;


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

	private static JToggleButton selectButton, wireButton, selectSpecialButton, panButton, zoomButton, outlineButton;
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
	private static Cursor outlineCursor = null;
	private static ToolBar toolbar;

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

		// the "Outline edit mode" button
		outlineButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonOutline.gif")));
		outlineButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { outlineEditCommand(); } });
		outlineButton.setToolTipText("Outline Edit");
		toolbar.add(outlineButton);
		modeGroup.add(outlineButton);

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

		// the "Options" buttons
		JButton editOptionButton = Button.newInstance(new ImageIcon(toolbar.getClass().getResource("ButtonOptionEdit.gif")));
		editOptionButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.editOptionsCommand(); } });
		editOptionButton.setToolTipText("Edit Options");
		toolbar.add(editOptionButton);

		JButton toolOptionButton = Button.newInstance(new ImageIcon(toolbar.getClass().getResource("ButtonOptionTool.gif")));
		toolOptionButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.toolOptionsCommand(); } });
		toolOptionButton.setToolTipText("Tool Options");
		toolbar.add(toolOptionButton);

		JButton ioOptionButton = Button.newInstance(new ImageIcon(toolbar.getClass().getResource("ButtonOptionIO.gif")));
		ioOptionButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.ioOptionsCommand(); } });
		ioOptionButton.setToolTipText("I/O Options");
		toolbar.add(ioOptionButton);

		// a separator
		toolbar.addSeparator();

		// the "Expanded" button
		JButton expandButton = Button.newInstance(new ImageIcon(toolbar.getClass().getResource("ButtonExpand.gif")));
		expandButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.expandOneLevelDownCommand(); } });
		expandButton.setToolTipText("Expand Cell Instances");
		toolbar.add(expandButton);

		// the "Unexpanded" button
		JButton unExpandButton = Button.newInstance(new ImageIcon(toolbar.getClass().getResource("ButtonUnexpand.gif")));
		unExpandButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { MenuCommands.unexpandOneLevelUpCommand(); } });
		unExpandButton.setToolTipText("Unexpand Cell Instances");
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

	/**
	 * Method called when the "select" button is pressed.
	 */
	public static void selectCommand()
	{
		EditWindow.setListener(ClickAndDragListener.theOne);
		TopLevel.setCurrentCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		curMode = CursorMode.SELECT;
		selectButton.setSelected(true);
		MenuCommands.cursorSelect.setSelected(true);
	}

	/**
	 * Method called when the "wiring" button is pressed.
	 */
	public static void wiringCommand()
	{
		EditWindow.setListener(WiringListener.theOne);
		makeCursors();
		TopLevel.setCurrentCursor(wiringCursor);
		curMode = CursorMode.WIRE;
		wireButton.setSelected(true);
		MenuCommands.cursorWiring.setSelected(true);
	}

	/**
	 * Method called when the "select special" button is pressed.
	 */
	public static void selectSpecialCommand()
	{
		EditWindow.setListener(ClickAndDragListener.theOne);
		makeCursors();
		TopLevel.setCurrentCursor(specialSelectCursor);
		curMode = CursorMode.SELECTSPECIAL;
		selectSpecialButton.setSelected(true);
		MenuCommands.cursorSelectSpecial.setSelected(true);
	}

	/**
	 * Method called when the "pan" button is pressed.
	 */
	public static void panCommand()
	{
		EditWindow.setListener(ZoomAndPanListener.theOne);
		makeCursors();
		TopLevel.setCurrentCursor(panCursor);
		curMode = CursorMode.PAN;
		panButton.setSelected(true);
		MenuCommands.cursorPan.setSelected(true);
	}

	/**
	 * Method called when the "zoom" button is pressed.
	 */
	public static void zoomCommand()
	{
		EditWindow.setListener(ZoomAndPanListener.theOne);
		TopLevel.setCurrentCursor(zoomCursor);
		curMode = CursorMode.ZOOM;
		zoomButton.setSelected(true);
		MenuCommands.cursorZoom.setSelected(true);
	}

	/**
	 * Method called when the "outline edit" button is pressed.
	 */
	public static void outlineEditCommand()
	{
		NodeInst ni = (NodeInst)Highlight.getOneElectricObject(NodeInst.class);
		if (ni == null)
		{
			System.out.println("Must first select a node with outline capabilities");
			selectCommand();
			return;
		}
		if (!ni.getProto().isHoldsOutline())
		{
			System.out.println("Cannot edit outline information on " + ni.getProto().describe() + " nodes");
			selectCommand();
			return;
		}

		EditWindow.setListener(OutlineListener.theOne);
		OutlineListener.theOne.setNode(ni);
		TopLevel.setCurrentCursor(outlineCursor);
		curMode = CursorMode.OUTLINE;
		outlineButton.setSelected(true);
		MenuCommands.cursorOutline.setSelected(true);
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
		fullButton.setSelected(true);
		MenuCommands.moveFull.setSelected(true);
	}

	/**
	 * Method called when the "half arrow distance" button is pressed.
	 */
	public static void halfArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.HALF;
		halfButton.setSelected(true);
		MenuCommands.moveHalf.setSelected(true);
	}

	/**
	 * Method called when the "quarter arrow distance" button is pressed.
	 */
	public static void quarterArrowDistanceCommand()
	{
		curArrowDistance = ArrowDistance.QUARTER;
		quarterButton.setSelected(true);
		MenuCommands.moveQuarter.setSelected(true);
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
		objectsButton.setSelected(true);
		MenuCommands.selectObjects.setSelected(true);
	}

	/**
	 * Method called when the "select area" button is pressed.
	 */
	public static void selectAreaCommand()
	{
		curSelectMode = SelectMode.AREA;
		areaButton.setSelected(true);
		MenuCommands.selectArea.setSelected(true);
	}

	/**
	 * Method to tell what selection mode is in effect.
	 * @return the current selection mode (objects or area).
	 */
	public static SelectMode getSelectMode() { return curSelectMode; }

}
