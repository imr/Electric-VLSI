/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Main.java
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
package com.sun.electric;

import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.change.Undo;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.MenuCommands;
import com.sun.electric.tool.user.dialogs.EditOptions;
import com.sun.electric.tool.user.help.HelpViewer;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.ImageIcon;
import javax.swing.BorderFactory;
import javax.swing.JLabel;

// these may not exist on non-Macintosh platforms, and are stubbed-out in "AppleJavaExtensions.jar"
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * This class initializes Electric and starts the system.
 */
public final class Main
{
    private static boolean DEBUG;   // global debug flag

	private Main() {}

	/**
	 * The main entry point of Electric.
	 * @param args the arguments to the program.
	 */
	public static void main(String[] args)
	{
		// initialize Mac OS 10 if applicable
		MacOSXInterface.registerMacOSXApplication();

		SplashWindow sw = new SplashWindow();

		// convert args to array list
        List argsList = new ArrayList();
        for (int i=0; i<args.length; i++) argsList.add(args[i]);

		// -m for multiple windows
		TopLevel.OSInitialize(hasCommandLineOption(argsList, "-m"));

		// initialize database
		new InitDatabase(argsList, sw);
	}

	/**
	 * Class to display a Splash Screen at the start of the program.
	 */
	private static class SplashWindow extends JFrame
	{
		public SplashWindow()
		{
			super();
			setUndecorated(true);

			JPanel whole = new JPanel();
			whole.setBorder(BorderFactory.createLineBorder(new Color(0, 170, 0), 5));
			whole.setLayout(new BorderLayout());

			JLabel l = new JLabel(new ImageIcon(LibFile.getLibFile("SplashImage.gif")));
			whole.add(l, BorderLayout.CENTER);
			JLabel v = new JLabel("Version " + Version.getVersion(), JLabel.CENTER);
			whole.add(v, BorderLayout.SOUTH);
			Font font = new Font(User.getDefaultFont(), Font.BOLD, 24);
			v.setFont(font);
			v.setForeground(Color.BLACK);
			v.setBackground(Color.WHITE);

			getContentPane().add(whole, BorderLayout.SOUTH);
			pack();
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension labelSize = getPreferredSize();
			setLocation(screenSize.width/2 - (labelSize.width/2),
				screenSize.height/2 - (labelSize.height/2));
			WindowsEvents windowsEvents = new WindowsEvents(this);
			addWindowListener(windowsEvents);
			setVisible(true);
		}
	}

	/**
	 * This class handles deactivation of the splash screen and forces it back to the top.
	 */
	private static class WindowsEvents implements WindowListener
	{
		SplashWindow sw;

		WindowsEvents(SplashWindow sw)
		{
			super();
			this.sw = sw;
		}

		public void windowActivated(WindowEvent e) {}
		public void windowClosed(WindowEvent e) {}
		public void windowClosing(WindowEvent e) {}
		public void windowDeiconified(WindowEvent e) {}
		public void windowIconified(WindowEvent e) {}
		public void windowOpened(WindowEvent e) {}

		public void windowDeactivated(WindowEvent e)
		{
			sw.toFront();
		}
	}


    /** check if command line option 'option' present in 
     * command line args. If present, return true and remove if from the list.
     * Otherwise, return false.
     */
    private static boolean hasCommandLineOption(List argsList, String option) 
    {
        for (int i=0; i<argsList.size(); i++) {
            if (((String)argsList.get(i)).equals(option)) {
                argsList.remove(i);
                return true;
            }
        }
        return false;
    }

    /** get command line option for 'option'. Returns null if 
     * no such 'option'.  If found, remove it from the list.
     */
    private static String getCommandLineOption(List argsList, String option)
    {
        for (int i=0; i<argsList.size()-1; i++) {
            if (((String)argsList.get(i)).equals(option)) {
                argsList.remove(i); // note that this shifts objects in arraylist
                // check if next string valid (i.e. no dash)
                if (((String)argsList.get(i)).startsWith("-")) {
                    System.out.println("Bad command line option: "+ option +" "+ argsList.get(i+1));
                    return null;
                }
                return (String)argsList.remove(i);
            }
        }
        return null;
    }

    /** open any libraries specified on the command line.  This method should be 
     * called after any valid options have been parsed out
     */
    private static void openCommandLineLibs(List argsList)
    {
        List fileURLs = new ArrayList();
        for (int i=0; i<argsList.size(); i++) {
            String arg = (String)argsList.get(i);
            if (arg.startsWith("-")) {
                System.out.println("Command line option "+arg+" not understood, ignoring.");
                continue;
            }
			URL url = TextUtils.makeURLToFile(arg);
            if (url == null) continue;
            fileURLs.add(url);
        }
        // open any libraries
        MenuCommands.ReadInitialELIBs job = new MenuCommands.ReadInitialELIBs(fileURLs);
    }

	/**
	 * Class to init all technologies.
	 */
	private static class InitDatabase extends Job
	{
		List argsList;
		SplashWindow sw;

		protected InitDatabase(List argsList, SplashWindow sw)
		{
			super("Init database", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.argsList = argsList;
			this.sw = sw;
			startJob();
		}

		public boolean doIt()
		{
            Undo.changesQuiet(true);

			// initialize all of the technologies
			Technology.initAllTechnologies();

			// initialize all of the tools
			Tool.initAllTools();

			// initialize the constraint system
			Layout con = Layout.getConstraint();
			Constraints.setCurrent(con);

            // do processing of arguments
            if (hasCommandLineOption(argsList, "-NOMINMEM")) {
                // do nothing, just consume option: handled in Launcher
            }
            if (hasCommandLineOption(argsList, "-debug")) {
                DEBUG = true;
            }
			String beanShellScript = getCommandLineOption(argsList, "-s");
			openCommandLineLibs(argsList);

			// run script
			if (beanShellScript != null) EvalJavaBsh.runScript(beanShellScript);

			// remove the splash screen
			sw.removeNotify();

            // pop up the tool tips window
            Preferences prefs = Preferences.userNodeForPackage(HelpViewer.class);
            boolean showTips = prefs.getBoolean(HelpViewer.showOnStartUp, true);
            if (showTips) {
                HelpViewer tip = new HelpViewer(TopLevel.getCurrentJFrame(), false, "Mouse Interface");
                tip.setVisible(true);
            }

            Undo.changesQuiet(false);
            return true;
		}
	}

    public static boolean getDebug() { return DEBUG; }

	/**
	 * Generic registration with the Mac OS X application menu.
	 * Attempts to register with the Apple EAWT.
	 * This method calls OSXAdapter.registerMacOSXApplication().
	 */
//	private static void macOSXRegistration()
//	{
//		try
//		{
//			Class osxAdapter = Class.forName("com.sun.electric.Main.MacOSXInterface");
//			Class[] defArgs = {};
//			Method registerMethod = osxAdapter.getDeclaredMethod("registerMacOSXApplication", defArgs);
//			if (registerMethod != null)
//			{
//				Object[] args = {};
//				registerMethod.invoke(osxAdapter, args);
//			}
//		} catch (NoClassDefFoundError e)
//		{
//			System.err.println("This version of Mac OS X does not support the Apple EAWT (" + e + ")");
//		} catch (ClassNotFoundException e)
//		{
//			// If the class is not found, then perhaps this isn't a Macintosh, and we should stop
//		} catch (Exception e)
//		{
//			System.err.println("Exception while loading the OSXAdapter:");
//			e.printStackTrace();
//		}
//	}

	/**
	 * Class for initializing the Macintosh OS X world.
	 */
	static class MacOSXInterface extends ApplicationAdapter
	{
		private static MacOSXInterface adapter = null;
		private static Application application = null;

		private MacOSXInterface () {}

		/**
		 * Method called when the "About" item is selected in the Macintosh "Electric" menu.
		 */
		public void handleAbout(ApplicationEvent ae)
		{
			ae.setHandled(true);
			MenuCommands.aboutCommand();
		}

		/**
		 * Method called when the "Preferences" item is selected in the Macintosh "Electric" menu.
		 */
		public void handlePreferences(ApplicationEvent ae)
		{
			ae.setHandled(true);
			EditOptions.editOptionsCommand();
		}

		/**
		 * Method called when the "Quit" item is selected in the Macintosh "Electric" menu.
		 */
		public void handleQuit(ApplicationEvent ae)
		{
			ae.setHandled(false);
			MenuCommands.quitCommand();
		}

		/**
		 * Method to initialize the Macintosh OS X environment.
		 */
		public static void registerMacOSXApplication()
		{
			// tell it to use the system menubar
			System.setProperty("com.apple.macos.useScreenMenuBar", "true");
			System.setProperty("apple.laf.useScreenMenuBar", "true");

			// set the name of the leftmost pulldown menu to "Electric"
			System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Electric");

			// create Mac objects for handling the "Electric" menu
			if (application == null) application = new com.apple.eawt.Application();
			if (adapter == null) adapter = new MacOSXInterface();
			application.addApplicationListener(adapter);
			application.setEnabledPreferencesMenu(true);
		}
	} 

}
