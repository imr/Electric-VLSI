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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.user.UserMenuCommands;
import com.sun.electric.tool.user.ui.PaletteFrame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.Method;

import javax.swing.JFrame;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenuBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.ImageIcon;


/**
 * Class to define a top-level window.
 * In MDI mode (used by Windows to group multiple documents into a single window) this class is
 * used for that single top window.
 * In SDI mode (used elsewhere to give each cell its own window) this class is used many times for each window.
 */
public class TopLevel extends JFrame
{
	/**
	 * OS is a typesafe enum class that describes the current operating system.
	 */
	public static class OS
	{
		private final String name;

		private OS(String name) { this.name = name; }

		/**
		 * Returns a printable version of this OS.
		 * @return a printable version of this OS.
		 */
		public String toString() { return name; }

		/** Describes Windows. */							public static final OS WINDOWS   = new OS("Windows");
		/** Describes UNIX/Linux. */						public static final OS UNIX      = new OS("UNIX");
		/** Describes Macintosh. */							public static final OS MACINTOSH = new OS("Macintosh");
	}

	/** True if in MDI mode, otherwise SDI. */				private static boolean mdi;
	/** The desktop pane (if MDI). */						private static JDesktopPane desktop;
	/** The main frame (if MDI). */							private static TopLevel topLevel;
	/** The only status bar (if MDI). */					private StatusBar sb;
	/** The EditWindow associated with this (if SDI). */	private EditWindow wnd;
	/** The size of the screen. */							private static Dimension scrnSize;
	/** The current operating system. */					private static OS os;
	/** The palette object system. */						private static PaletteFrame palette;
	/** The cursor being displayed. */						private static Cursor cursor;

	/**
	 * Constructor to build a window.
	 * @param name the title of the window.
	 */
	public TopLevel(String name, Dimension screenSize, WindowFrame frame)
	{
		super(name);
		setSize(screenSize);
		getContentPane().setLayout(new BorderLayout());
		setVisible(true);

		// set an icon on the window
		setIconImage(new ImageIcon(getClass().getResource("IconElectric.gif")).getImage());

		// create the menu bar
		JMenuBar menuBar = UserMenuCommands.createMenuBar();
		setJMenuBar(menuBar);

		// create the tool bar
		ToolBar toolBar = ToolBar.createToolBar();
		getContentPane().add(toolBar, BorderLayout.NORTH);

		// create the status bar
		sb = new StatusBar(frame);
		getContentPane().add(sb, BorderLayout.SOUTH);

		if (isMDIMode())
		{
			addWindowListener(new WindowsEvents());
		} else
		{
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		}

		cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	}

	/**
	 * Method to initialize the window system.
	 */
	public static void Initialize()
	{
		// initialize the messages window
		MessagesWindow cl = new MessagesWindow(scrnSize);
		palette = PaletteFrame.newInstance();
	}

	/**
	 * Method to initialize the window system.
	 */
	public static void OSInitialize()
	{
		// setup the size of the screen
		scrnSize = (Toolkit.getDefaultToolkit()).getScreenSize();

		// setup specific look-and-feel
		mdi = false;
		try{
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows"))
			{
				os = OS.WINDOWS;
				mdi = true;
				scrnSize.height -= 30;
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			} else if (osName.startsWith("linux") || osName.startsWith("solaris") || osName.startsWith("sunos"))
			{
				os = OS.UNIX;
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MotifLookAndFeel");
			} else if (osName.startsWith("mac"))
			{
				os = OS.MACINTOSH;
				System.setProperty("com.apple.macos.useScreenMenuBar", "true");
				System.setProperty("apple.laf.useScreenMenuBar", "true");
				System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Electric");
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MacLookAndFeel");
				macOSXRegistration();
			}
		} catch(Exception e) {}

		// in MDI, create the top frame now
		if (isMDIMode())
		{
			topLevel = new TopLevel("Electric", scrnSize, null);	

			// make the desktop
			desktop = new JDesktopPane();
			topLevel.getContentPane().add(desktop, BorderLayout.CENTER);
		}
	}

	/**
	 * Method to tell which operating system Electric is running on.
	 * @return the operating system Electric is running on.
	 */
	public static OS getOperatingSystem() { return os; }

	/**
	 * Method to tell whether Electric is running in SDI or MDI mode.
	 * SDI is Single Document Interface, where each document appears in its own window.
	 * This is used on UNIX/Linux and on Macintosh.
	 * MDI is Multiple Document Interface, where the main window has all documents in it as subwindows.
	 * This is used on Windows.
	 * @return true if Electric is in MDI mode.
	 */
	public static boolean isMDIMode() { return mdi; }

	/**
	 * Method to return component palette window.
	 * The component palette is the vertical toolbar on the left side.
	 * @return the component palette window.
	 */
	public static PaletteFrame getPaletteFrame() { return palette; }

	/**
	 * Method to return the only TopLevel frame.
	 * This applies only in MDI mode.
	 * @return the only TopLevel frame.
	 */
	public static TopLevel getTopLevel() { return topLevel; }

	/**
	 * Method to return status bar associated with this TopLevel.
	 * @return the status bar associated with this TopLevel.
	 */
	public StatusBar getStatusBar() { return sb; }

	/**
	 * Method to return the size of the screen that Electric is on.
	 * @return the size of the screen that Electric is on.
	 */
	public static Dimension getScreenSize() { return new Dimension(scrnSize); }

	/**
	 * Method to add an internal frame to the desktop.
	 * This only makes sense in MDI mode, where the desktop has multiple subframes.
	 * @param jif the internal frame to add.
	 */
	public static void addToDesktop(JInternalFrame jif) { desktop.add(jif); }

	public static Cursor getCurrentCursor() { return cursor; }

	public static void setCurrentCursor(Cursor cursor)
	{
		TopLevel.cursor = cursor;
		JFrame jf = TopLevel.getCurrentJFrame();
		jf.setCursor(cursor);
		palette.getPanel().setCursor(cursor);
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			EditWindow wnd = wf.getEditWindow();
			wnd.setCursor(cursor);
		}
	}

	/**
	 * Method to return the current EditWindow.
	 * @return the current EditWindow.
	 */
	public static JFrame getCurrentJFrame()
	{
		if (isMDIMode())
        {
			return topLevel;
 		} else
        {
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			return wf.getFrame();
        }
	}

	/**
	 * Method to return the EditWindow associated with this top-level window.
	 * This only makes sense for SDI applications where a WindowFrame is inside of a TopLevel.
	 * @return the EditWindow associated with this top-level window.
	 */
	public EditWindow getEditWindow() { return wnd; }

	/**
	 * Method to set the edit window associated with this top-level window.
	 * This only makes sense for SDI applications where a WindowFrame is inside of a TopLevel.
	 * @param wnd the EditWindow to associatd with this.
	 */
	public void setEditWindow(EditWindow wnd) { this.wnd = wnd; }

	/**
	 * This class handles close events for JFrame objects (used in MDI mode to quit).
	 */
	static class WindowsEvents extends WindowAdapter
	{
		WindowsEvents() { super(); }

		public void windowClosing(WindowEvent evt) { UserMenuCommands.quitCommand(); }
	}
	
	/**
	 * Generic registration with the Mac OS X application menu.
	 * Attempts to register with the Apple EAWT.
	 * This method calls OSXAdapter.registerMacOSXApplication().
	 */
	private static void macOSXRegistration()
	{
		try
		{
			Class osxAdapter = Class.forName("com.sun.electric.tool.user.ui.OSXAdapter");
			Class[] defArgs = {};
			Method registerMethod = osxAdapter.getDeclaredMethod("registerMacOSXApplication", defArgs);
			if (registerMethod != null)
			{
				Object[] args = {};
				registerMethod.invoke(osxAdapter, args);
			}
		} catch (NoClassDefFoundError e)
		{
			// This will be thrown first if the OSXAdapter is loaded on a system without the EAWT
			// because OSXAdapter extends ApplicationAdapter in its def
			System.err.println("This version of Mac OS X does not support the Apple EAWT.  Application Menu handling has been disabled (" + e + ")");
		} catch (ClassNotFoundException e)
		{
			// This shouldn't be reached; if there's a problem with the OSXAdapter we should get the 
			// above NoClassDefFoundError first.
			System.err.println("This version of Mac OS X does not support the Apple EAWT.  Application Menu handling has been disabled (" + e + ")");
		} catch (Exception e)
		{
			System.err.println("Exception while loading the OSXAdapter:");
			e.printStackTrace();
		}
	}
	
}
