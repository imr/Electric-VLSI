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
import java.awt.Insets;
import java.awt.event.KeyEvent;

/**
 * This class manages the Electric toolbar.
 */
public class ToolBar extends JToolBar
{
	/**
	 * Mode is a typesafe enum class that describes the current editing mode (select, zoom, etc).
	 */
	public static class Mode
	{
		private String name;

		private Mode(String name) { this.name = name; }

		public String toString() { return "Mode="+name; }

		/** Describes Selection mode (click and drag). */		public static final Mode SELECT = new Mode("select");
		/** Describes Selection mode (click and drag). */		public static final Mode SELECTSPECIAL = new Mode("select");
		/** Describes Panning mode (move window contents). */	public static final Mode PAN = new Mode("pan");
		/** Describes Zoom mode (scale window contents). */		public static final Mode ZOOM = new Mode("zoom");
	}

	private static JToggleButton selectButton;
	private static JToggleButton selectSpecialButton;
	private static JToggleButton panButton;
	private static JToggleButton zoomButton;
	private static ButtonGroup modeGroup;
	private static Mode curMode = Mode.SELECT;

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
//		selectButton.setBorderPainted(false);
		selectButton.setSelected(true);
		toolbar.add(selectButton);
		modeGroup.add(selectButton);

		// the "Special select mode" button
		selectSpecialButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonSelectSpecial.gif")));
		selectSpecialButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { selectSpecialCommand(); } });
		selectSpecialButton.setToolTipText("Special Select");
//		selectSpecialButton.setBorderPainted(false);
		toolbar.add(selectSpecialButton);
		modeGroup.add(selectSpecialButton);

		// the "Pan mode" button
		panButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonPan.gif")));
		panButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { panCommand(); } });
		panButton.setToolTipText("Pan");
//		panButton.setBorderPainted(false);
		toolbar.add(panButton);
		modeGroup.add(panButton);

		// the "Zoom mode" button
		zoomButton = new JToggleButton(new ImageIcon(toolbar.getClass().getResource("ButtonZoom.gif")));
		zoomButton.addActionListener(
			new ActionListener() { public void actionPerformed(ActionEvent e) { zoomCommand(); } });
		zoomButton.setToolTipText("Zoom");
//		zoomButton.setBorderPainted(false);
		toolbar.add(zoomButton);
		modeGroup.add(zoomButton);

		// a separator
		toolbar.addSeparator();

		// a test button
		Button testButton = Button.newInstance(new ImageIcon(toolbar.getClass().getResource("ButtonTest.gif")));
		testButton.setToolTipText("test");

		// set an area for popup menu to be triggered within a button
		Insets insets = new Insets(22,22,32,32);
		JPopupMenu popup = new JPopupMenu();
		JMenuItem testItem = new JMenuItem("test 1");
		popup.add(testItem);
		testItem = new JMenuItem("test 2");
		popup.add(testItem);
		testButton.addPopupMenu(popup, insets);
		toolbar.add(testButton);

		// return the toolbar
		return toolbar;
	}

	/**
	 * Routine called when the "select" button is pressed.
	 */
	public static void selectCommand() { curMode = Mode.SELECT; }

	/**
	 * Routine called when the "select special" button is pressed.
	 */
	public static void selectSpecialCommand() { curMode = Mode.SELECTSPECIAL; }

	/**
	 * Routine called when the "pan" button is pressed.
	 */
	public static void panCommand() { curMode = Mode.PAN; }

	/**
	 * Routine called when the "zoom" button is pressed.
	 */
	public static void zoomCommand() { curMode = Mode.ZOOM; }

	/**
	 * Routine to tell which cursor mode is in effect.
	 * @return the current mode (select, pan, or zoom).
	 */
	public static Mode getMode() { return curMode; }

}
