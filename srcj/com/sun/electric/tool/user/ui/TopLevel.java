/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TopLevel.java
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

import com.sun.electric.tool.user.UserMenuCommands;
import com.sun.electric.tool.user.ui.ToolBar;

import java.io.File;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JDesktopPane;
import javax.swing.JMenuBar;
import javax.swing.UIManager;
import javax.swing.ImageIcon;


/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TopLevel extends JFrame
{
	private static JDesktopPane desktop;
	private static TopLevel topLevel;
	private static String libdir;

	private TopLevel(String s)
	{
		super(s);
	}

	public static void Initialize()
	{
		// setup specific look-and-feel
		try{
			String os = System.getProperty("os.name").toLowerCase();
			if (os.startsWith("windows"))
			{
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			} else if (os.startsWith("linux") || os.startsWith("solaris"))
			{
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MotifLookAndFeel");
			} else if (os.startsWith("mac"))
			{
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MacLookAndFeel");
			}
		} catch(Exception e) {}

		// figure out the library directory
		libdir = null;
		File dir1 = new File ("./lib");
		try
		{
			libdir = dir1.getCanonicalPath();
		} catch(Exception e) { libdir = null; }
		if (libdir == null)
		{
			libdir = "C:\\DevelE\\Electric\\lib\\";
		}

		// make the top-level window
		topLevel = new TopLevel("Electric");	
		topLevel.AddWindowExit();
		Dimension scrnSize = (Toolkit.getDefaultToolkit()).getScreenSize();
		scrnSize.height -= 30;
		topLevel.setSize(scrnSize);

		// set an icon with the application
		topLevel.setIconImage(new ImageIcon(libdir + "ElectricIcon.gif").getImage());

		// make the desktop
		desktop = new JDesktopPane();
		topLevel.getContentPane().setLayout(new BorderLayout());
		topLevel.getContentPane().add(desktop, BorderLayout.CENTER);
		topLevel.setVisible(true);

		// create the menu bar
		JMenuBar menuBar = UserMenuCommands.createMenuBar();
		topLevel.setJMenuBar(menuBar);

		// create the tool bar
		ToolBar toolBar = ToolBar.createToolBar();
		topLevel.getContentPane().add(toolBar, BorderLayout.NORTH);

		// create the messages window
		MessagesWindow cl = new MessagesWindow(desktop.getSize());
		desktop.add(cl.getFrame());
	}

	public static JDesktopPane getDesktop() { return desktop; }

	public static String getLibDir() { return libdir; }

	public static EditWindow getCurrentEditWindow()
	{
		WindowFrame wf = (WindowFrame)desktop.getSelectedFrame();
		if (wf == null) return null;
		return wf.getEditWindow();
	}

	public void AddWindowExit()
	{
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				System.exit(0);
			}
		});
	}
	
}
