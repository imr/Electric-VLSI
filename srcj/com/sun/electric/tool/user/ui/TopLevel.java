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

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.menus.MenuBar;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.Main;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import javax.swing.*;


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

    /**
     * Describe the windowing mode.  The current modes are MDI and SDI.
     */
    public static class Mode
    {
        private final String name;
        private Mode(String name) { this.name = name; }

        public String toString() { return name; }
        public static final Mode MDI = new Mode("MDI");
        public static final Mode SDI = new Mode("SDI");
    }

	/** True if in MDI mode, otherwise SDI. */				private static Mode mode;
	/** The desktop pane (if MDI). */						private static JDesktopPane desktop = null;
	/** The main frame (if MDI). */							private static TopLevel topLevel = null;
	/** The only status bar (if MDI). */					private StatusBar sb = null;
	/** The WindowFrame associated with this (if SDI). */	private WindowFrame wf = null;
	/** The size of the screen. */							private static Dimension scrnSize;
	/** The current operating system. */					private static OS os;
	/** The palette object. */								private static PaletteFrame palette;
	/** The messages window. */								private static MessagesWindow messages;
	/** The cursor being displayed. */						private static Cursor cursor;
    /** If the busy cursor is overriding the normal cursor */ private static boolean busyCursorOn = false;

    /** The menu bar */                                     private MenuBar menuBar;
    /** The tool bar */                                     private ToolBar toolBar;
    
	/**
	 * Constructor to build a window.
	 * @param name the title of the window.
	 */
	public TopLevel(String name, Rectangle bound, WindowFrame frame, GraphicsConfiguration gc)
	{
		super(name, gc);
		setLocation(bound.x, bound.y);
		setSize(bound.width, bound.height);
		getContentPane().setLayout(new BorderLayout());

		// set an icon on the window
		setIconImage(getFrameIcon().getImage());

		// create the menu bar
		menuBar = MenuCommands.createMenuBar();
		setJMenuBar(menuBar);

		// create the tool bar
		toolBar = ToolBar.createToolBar();
		getContentPane().add(toolBar, BorderLayout.NORTH);

		// create the status bar
		sb = new StatusBar(frame);
		getContentPane().add(sb, BorderLayout.SOUTH);

		if (isMDIMode())
		{
			addWindowListener(new WindowsEvents());
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			addComponentListener(new ReshapeComponentAdapter());
 		} else
		{
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            //setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            addWindowFocusListener(EDialog.dialogFocusHandler);
		}

		cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

		// For 3D: LightWeight v/s heavy: mixing awt and swing
		try {
			javax.swing.JPopupMenu.setDefaultLightWeightPopupEnabled(false);
			javax.swing.ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
			enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Method to return the Icon to use in windows.
	 * @return the Icon to use in windows.
	 */
	public static ImageIcon getFrameIcon()
	{
		return Resources.getResource(TopLevel.class, "IconElectric.gif");
	}

	/**
	 * Method to initialize the window system with the specified mode.
     * If mode is null, the mode is implied by the operating system.
	 */
	public static void InitializeWindows()
	{
		// initialize the messages window and palette
        messages = new MessagesWindow();
        palette = PaletteFrame.newInstance();
        palette.loadForTechnology();
        WindowFrame.createEditWindow(null);
    }

	private static Pref cacheWindowLoc = Pref.makeStringPref("WindowLocation", User.tool.prefs, "");

	/**
	 * Method to initialize the window system.
	 */
	public static void OSInitialize(Mode mode)
	{
		// setup the size of the screen
		scrnSize = (Toolkit.getDefaultToolkit()).getScreenSize();

		// setup specific look-and-feel
        Mode osMode = null;
		try{
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows"))
			{
				os = OS.WINDOWS;
				osMode = Mode.MDI;
				scrnSize.height -= 30;
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");

				// force simple newline characters
				System.setProperty("line.separator", "\n");
			} else if (osName.startsWith("linux") || osName.startsWith("solaris") || osName.startsWith("sunos"))
			{
				os = OS.UNIX;
                osMode = Mode.SDI;
                //UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			} else if (osName.startsWith("mac"))
			{
				os = OS.MACINTOSH;
                osMode = Mode.SDI;
				UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.MacLookAndFeel");
			}
		} catch(Exception e) {}

        // set the windowing mode
        if (mode == null)
            TopLevel.mode = osMode;
        else
            TopLevel.mode = mode;
        //TopLevel.mode = Mode.MDI;

        // set current working directory
        String setting = User.getInitialWorkingDirectorySetting();
        if (setting.equals(User.INITIALWORKINGDIRSETTING_BASEDONOS)) {
            // default is last used dir
            if (os == OS.UNIX) {
                // switch to current dir
                User.setWorkingDirectory(System.getProperty("user.dir"));
            }
        } else if (setting.equals(User.INITIALWORKINGDIRSETTING_USECURRENTDIR))
            User.setWorkingDirectory(System.getProperty("user.dir"));
        // else
            // default is to use last used dir

		// in MDI, create the top frame now
		if (isMDIMode())
		{
			String loc = cacheWindowLoc.getString();
			Rectangle bound = parseBound(loc);
			if (bound == null)
				bound = new Rectangle(scrnSize);
			topLevel = new TopLevel("Electric", bound, null, null);
			// make the desktop
			desktop = new JDesktopPane();
			topLevel.getContentPane().add(desktop, BorderLayout.CENTER);
            if (!Main.BATCHMODE) topLevel.setVisible(true);
		}
	}

	private static Rectangle parseBound(String loc)
	{
		int lowX = TextUtils.atoi(loc);
		int commaPos = loc.indexOf(',');
		if (commaPos < 0) return null;
		int lowY = TextUtils.atoi(loc.substring(commaPos+1));
		int spacePos = loc.indexOf(' ');
		if (spacePos < 0) return null;
		int width = TextUtils.atoi(loc.substring(spacePos+1));
		int xPos = loc.indexOf('x');
		if (xPos < 0) return null;
		int height = TextUtils.atoi(loc.substring(xPos+1));
		return new Rectangle(lowX, lowY, width, height);
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
	public static boolean isMDIMode() { return (mode == Mode.MDI); }

	/**
	 * Method to return component palette window.
	 * The component palette is the vertical toolbar on the left side.
	 * @return the component palette window.
	 */
	public static PaletteFrame getPaletteFrame() { return palette; }

	/**
	 * Method to return messages window.
	 * The messages window runs along the bottom.
	 * @return the messages window.
	 */
	public static MessagesWindow getMessagesWindow() { return messages; }

	/**
	 * Method to return status bar associated with this TopLevel.
	 * @return the status bar associated with this TopLevel.
	 */
	public StatusBar getStatusBar() { return sb; }

    /**
     * Get the tool bar associated with this TopLevel
     * @return the ToolBar.
     */
    public ToolBar getToolBar() { return toolBar; }

    /** Get the Menu Bar. Unfortunately named because getMenuBar() already exists */
    public MenuBar getTheMenuBar() { return menuBar; }

	/**
	 * Method to return the size of the screen that Electric is on.
	 * @return the size of the screen that Electric is on.
	 */
	public static Dimension getScreenSize()
	{
		if (isMDIMode())
		{
			Rectangle bounds = topLevel.getBounds();
			Rectangle dBounds = desktop.getBounds();
			if (dBounds.width != 0 && dBounds.height != 0)
			{
				return new Dimension(dBounds.width, dBounds.height);
			}
			return new Dimension(bounds.width-8, bounds.height-96);
		}
		return new Dimension(scrnSize);
	}

	/**
	 * Method to add an internal frame to the desktop.
	 * This only makes sense in MDI mode, where the desktop has multiple subframes.
	 * @param jif the internal frame to add.
	 */
	public static void addToDesktop(JInternalFrame jif) {
        AddToDesktopSafe addsafe = new AddToDesktopSafe(jif);
        if (desktop.isVisible() && !SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(new AddToDesktopSafe(jif));
        } else {
            addsafe.run();
        }
    }

    private static class AddToDesktopSafe implements Runnable {
        private JInternalFrame jif;
        private AddToDesktopSafe(JInternalFrame jif) { this.jif = jif; }
        public void run() {
            desktop.add(jif);
	        if (!Main.BATCHMODE) jif.show();
        }
    }

	public static JDesktopPane getDesktop() { return desktop; }

	public static Cursor getCurrentCursor() { return cursor; }

	public static synchronized void setCurrentCursor(Cursor cursor)
	{
        TopLevel.cursor = cursor;
        setCurrentCursorPrivate(cursor);
    }

    private static synchronized void setCurrentCursorPrivate(Cursor cursor)
    {
        if (mode == Mode.MDI) {
            JFrame jf = TopLevel.getCurrentJFrame();
            jf.setCursor(cursor);
        }
        if (palette != null) palette.setCursor(cursor);
        for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = (WindowFrame)it.next();
            wf.setCursor(cursor);
        }
	}

    /**
     * The busy cursor overrides any other cursor.
     * Call clearBusyCursor to reset to last set cursor
     */
    public static synchronized void setBusyCursor(boolean on) {
        if (on) {
            if (!busyCursorOn)
                setCurrentCursorPrivate(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            busyCursorOn = true;
        } else {
            // if the current cursor is a busy cursor, set it to the last normal cursor
            if (busyCursorOn)
                setCurrentCursorPrivate(getCurrentCursor());
            busyCursorOn = false;
        }
    }

	/**
	 * Method to return the current JFrame on the screen.
	 * @return the current JFrame.
	 */
	public static TopLevel getCurrentJFrame()
	{
		if (isMDIMode())
        {
			return topLevel;
 		} else
        {
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
            if (wf == null) return null;
			return wf.getFrame();
        }
	}

	/**
	 * Method to set the WindowFrame associated with this top-level window.
	 * This only makes sense for SDI applications where a WindowFrame is inside of a TopLevel.
	 * @param wf the WindowFrame to associatd with this.
	 */
	public void setWindowFrame(WindowFrame wf) { this.wf = wf; }
    
    /**
     * Method called when done with this Frame.  Both the menuBar
     * and toolBar have persistent state in static hash tables to maintain
     * consistency across different menu bars and tool bars in SDI mode.
     * Those references must be nullified for garbage collection to reclaim
     * that memory.  This is really for SDI mode, because in MDI mode the 
     * TopLevel is only closed on exit, and all the application memory will be freed.
     * <p>
     * NOTE: JFrame does not get garbage collected after dispose() until
     * some arbitrary point later in time when the garbage collector decides
     * to free it.
     */
    public void finished()
    {
        //System.out.println(this.getClass()+" being disposed of");
        // clean up menubar
        setJMenuBar(null);
        // TODO: figure out why Swing still sends events to finished menuBars
        //menuBar.finished(); menuBar = null;
        // clean up toolbar
        getContentPane().remove(toolBar);
        toolBar.finished(); toolBar = null;
        // clean up scroll bar
        getContentPane().remove(sb);
        sb.finished(); sb = null;
        /* Note that this gets called from WindowFrame, and
            WindowFrame has a reference to EditWindow, so
            WindowFrame will call wnd.finished(). */
        wf = null;
        // dispose of myself
        super.dispose();
    }

    /**
     * Print error message <code>msg</code> and stack trace
     * if <code>print</code> is true.
     * @param print print error message and stack trace if true
     * @param msg error message to print
     */
    public static void printError(boolean print, String msg)
    {
        if (print) {
            Throwable t = new Throwable(msg);
            System.out.println(t.toString());
            ActivityLogger.logException(t);            
        }
    }

	private static class ReshapeComponentAdapter extends ComponentAdapter
	{
		public void componentMoved (ComponentEvent e) { saveLocation(e); }
		public void componentResized (ComponentEvent e) { saveLocation(e); }

		private void saveLocation(ComponentEvent e)
		{
			TopLevel frame = (TopLevel)e.getSource();
			Rectangle bounds = frame.getBounds();
			cacheWindowLoc.setString(bounds.getMinX() + "," + bounds.getMinY() + " " +
				bounds.getWidth() + "x" + bounds.getHeight());
		}
	}

	/**
	 * This class handles close events for JFrame objects (used in MDI mode to quit).
	 */
	private static class WindowsEvents extends WindowAdapter
	{
		WindowsEvents() { super(); }

		public void windowClosing(WindowEvent evt) {
            FileMenu.quitCommand();
        }
	}
}
