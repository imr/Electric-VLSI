/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UITopLevel.java
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;


/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class UITopLevel extends JFrame
{
	static JDesktopPane desktop;
	static UITopLevel topLevel;
	static String libdir;

	private UITopLevel()
	{
		super();
	}
	
	private UITopLevel(String s)
	{
		super(s);
	}
	
	public static void Initialize()
	{
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

		topLevel = new UITopLevel("Electric");	
		topLevel.AddWindowExit();
		Dimension scrnSize = (Toolkit.getDefaultToolkit()).getScreenSize();
		scrnSize.height -= 30;
		topLevel.setSize(scrnSize);

		// make the desktop
		desktop = new JDesktopPane();
		topLevel.getContentPane().add(desktop);
		topLevel.setVisible(true);

		// create the messages window
		UIMessages cl = new UIMessages(desktop.getSize());
		desktop.add(cl.getFrame());

		// figure out the library directory
		libdir = null;
		File dir1 = new File ("./lib");
		try
		{
			libdir = dir1.getCanonicalPath();
		} catch(Exception e) { libdir = null; }
		libdir = "C:\\DevelE\\Electric\\lib";

		topLevel.setIconImage(new ImageIcon(UITopLevel.getLibDir() + "\\ElectricIcon.gif").getImage());
	}
	
	public static void setMenuBar(JMenuBar menuBar)
	{
		topLevel.setJMenuBar(menuBar);
	}

	public static JDesktopPane getDesktop() { return desktop; }
	public static String getLibDir() { return libdir; }

	public void AddWindowExit()
	{
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e){
				System.exit(0);
			}
		});
	}
	
}
