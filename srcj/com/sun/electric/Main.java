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
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.change.Undo;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.menus.HelpMenu;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.TimerTask;

import javax.swing.*;

// these may not exist on non-Macintosh platforms, and are stubbed-out in "AppleJavaExtensions.jar"
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * This class initializes Electric and starts the system. How to run Electric:
 * <P>
 * <P> <CODE>java -jar electric.jar [electric-options]</CODE> without plugins
 * <P> <CODE>java -classpath electric.jar<i>delim</i>{list of plugins} com.sun.electric.Launcher [electric-options]</CODE>
 * otherwise, where <i>delim</i> is OS-dependant separator
 * <P> And Electric options are:
 * <P> <CODE>         -mdi: multiple document interface mode </CODE>
 * <P> <CODE>         -sdi: single document interface mode </CODE>
 * <P> <CODE>         -NOMINMEM: ignore minimum memory provided for JVM </CODE>
 * <P> <CODE>         -s script name: bean shell script to execute </CODE>
 * <P> <CODE>         -version: version information </CODE>
 * <P> <CODE>         -v: brief version information </CODE>
 * <P> <CODE>         -debug: debug mode. Extra information is available </CODE>
 * <P> <CODE>        -help: this message </CODE>
 * <P> <P>
 * See manual for more instructions.
 */
public final class Main
{
    private static boolean DEBUG;   // global debug flag
    public static boolean LOCALDEBUGFLAG; // Gilda's case
    public static boolean NOTHREADING = false;             // to turn off Job threading
	public static boolean BATCHMODE = false; // to run it in batch mode

	private Main() {}

	/**
	 * The main entry point of Electric.
	 * @param args the arguments to the program.
	 */
	public static void main(String[] args)
	{
		// convert args to array list
        List argsList = new ArrayList();
        for (int i=0; i<args.length; i++) argsList.add(args[i]);

		// -v (short version)
		if (hasCommandLineOption(argsList, "-v"))
		{
			System.out.println(Version.getVersion());
			System.exit(0);
		}

		// initialize Mac OS 10 if applicable
		MacOSXInterface.registerMacOSXApplication();

		// -version
		if (hasCommandLineOption(argsList, "-version"))
		{
			System.out.println(Version.getApplicationInformation());
			System.out.println("\t"+Version.getVersionInformation());
			System.out.println("\t"+Version.getAuthorInformation());
			System.out.println("\t"+Version.getCopyrightInformation());
			System.out.println("\t"+Version.getWarrantyInformation());
			System.exit(0);
		}

		// -help
        if (hasCommandLineOption(argsList, "-help"))
		{
	        System.out.println("Usage:");
	        System.out.println("  'java -jar electric.jar [electric-options]' without plugins");
	        System.out.println("  'java -classpath electric.jar<delim>{list of plugins} com.sun.electric.Launcher [electric-options]' otherwise, where <delim> is OS-dependant separator");
            System.out.println("\n\t-mdi: multiple document interface mode");
	        System.out.println("\t-sdi: single document interface mode");
	        System.out.println("\t-NOMINMEM: ignore minimum memory provided for JVM");
	        System.out.println("\t-s <script name>: bean shell script to execute");
	        System.out.println("\t-version: version information");
	        System.out.println("\t-v: brief version information");
	        System.out.println("\t-debug: debug mode. Extra information is available");
            System.out.println("\t-NOTHREADING: turn off Job threading.");
	        System.out.println("\t-batch: running in batch mode.");
	        System.out.println("\t-help: this message");

			System.exit(0);
		}

        ActivityLogger.initialize(true, true, false);
        //runThreadStatusTimer();
        EventProcessor ep = new EventProcessor();

		// initialize Mac OS 10 if applicable
		MacOSXInterface.registerMacOSXApplication();

		SplashWindow sw = null;


		// -debug for debugging
		if (hasCommandLineOption(argsList, "-debug")) DEBUG = true;
        if (hasCommandLineOption(argsList, "-gilda")) LOCALDEBUGFLAG = true;
        if (hasCommandLineOption(argsList, "-NOTHREADING")) NOTHREADING = true;
		if (hasCommandLineOption(argsList, "-batch")) BATCHMODE = true;

		if (!Main.BATCHMODE) sw = new SplashWindow();

        boolean mdiMode = hasCommandLineOption(argsList, "-mdi");
        boolean sdiMode = hasCommandLineOption(argsList, "-sdi");
        TopLevel.Mode mode = null;
        if (mdiMode) mode = TopLevel.Mode.MDI;
        if (sdiMode) mode = TopLevel.Mode.SDI;
		TopLevel.OSInitialize(mode);

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
			setTitle("Electric Splash");
			setIconImage(TopLevel.getFrameIcon().getImage());

			JPanel whole = new JPanel();
			whole.setBorder(BorderFactory.createLineBorder(new Color(0, 170, 0), 5));
			whole.setLayout(new BorderLayout());

			JLabel l = new JLabel(Resources.getResource(TopLevel.class, "SplashImage.gif"));
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
        // open any libraries but only when there is at least one
	    //if (fileURLs.size() > 0)
	    {
            FileMenu.ReadInitialELIBs job = new FileMenu.ReadInitialELIBs(fileURLs);
	    }
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
			try {
				Undo.changesQuiet(true);

				// initialize all of the technologies
				Technology.initAllTechnologies();

				// initialize all of the tools
				Tool.initAllTools();

				// initialize the constraint system
				Layout con = Layout.getConstraint();
				Constraints.setCurrent(con);

				// run script
				//if (beanShellScript != null) EvalJavaBsh.runScript(beanShellScript);
			} finally {
				Undo.changesQuiet(false);
			}

			if (hasCommandLineOption(argsList, "-NOMINMEM")) {
				// do nothing, just consume option: handled in Launcher
			}
			final String beanShellScript = getCommandLineOption(argsList, "-s");
			openCommandLineLibs(argsList);

            // finish initializing the GUI
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // remove the splash screen
                    if (sw != null) sw.removeNotify();
                    TopLevel.InitializeWindows();
                    // run script
                    if (beanShellScript != null) EvalJavaBsh.runScript(beanShellScript);
                }
            });
            return true;
		}
	}

	/**
	 * Method to tell whether Electric is running in "debug" mode.
	 * If the program is started with the "-debug" switch, debug mode is enabled.
	 * @return true if running in debug mode.
	 */
    public static boolean getDebug() { return DEBUG; }

    /**
     * Places a custom event processor on the event queue in order to
     * catch all exceptions generated by event processing.
     */
    public static class EventProcessor extends EventQueue
    {
        public EventProcessor() {
            Toolkit kit = Toolkit.getDefaultToolkit();
            kit.getSystemEventQueue().push(this);
        }

        protected void dispatchEvent(AWTEvent e) {
            try {
                super.dispatchEvent(e);
            }
            catch(Throwable ex) {
                ex.printStackTrace(System.err);
                ActivityLogger.logException(ex);
                if (ex instanceof Error) throw (Error)ex;
            }
        }
    }


    private static void runThreadStatusTimer() {
        int delay = 1000*60*10; // milliseconds
        Timer timer = new Timer(delay, new ThreadStatusTask());
        timer.start();
    }

    public static class ThreadStatusTask implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            Thread t = Thread.currentThread();
            ThreadGroup group = t.getThreadGroup();
            // get the top level group
            while (group.getParent() != null)
                group = group.getParent();
            Thread [] threads = new Thread[200];
            int numThreads = group.enumerate(threads, true);
            StringBuffer buf = new StringBuffer();
            for (int i=0; i<numThreads; i++) {
                buf.append("Thread["+i+"] "+threads[i]+": alive: "+threads[i].isAlive()+", interrupted: "+threads[i].isInterrupted()+"\n");
            }
            ActivityLogger.logThreadMessage(buf.toString());
        }
    }

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
			HelpMenu.aboutCommand();
		}

		/**
		 * Method called when the "Preferences" item is selected in the Macintosh "Electric" menu.
		 */
		public void handlePreferences(ApplicationEvent ae)
		{
			ae.setHandled(true);
			PreferencesFrame.preferencesCommand();
		}

		/**
		 * Method called when the "Quit" item is selected in the Macintosh "Electric" menu.
		 */
		public void handleQuit(ApplicationEvent ae)
		{
			ae.setHandled(false);
			FileMenu.quitCommand();
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
