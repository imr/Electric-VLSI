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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.menus.MenuBar;
import com.sun.electric.tool.user.menus.MenuBar.Menu;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Point2D;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
 * <P> <CODE>         -pulldowns: show list of all pulldown menus in Electric </CODE>
 * <P> <CODE>         -server: dump strace of snapshots</CODE>
 * <P> <CODE>         -help: this message </CODE>
 * <P> <P>
 * See manual for more instructions.
 */
public final class Main
{
	private static UserInterface currentUI;

    private Main() {}

	/**
	 * The main entry point of Electric.
	 * @param args the arguments to the program.
	 */
	public static void main(String[] args)
	{
		// convert args to array list
        List<String> argsList = new ArrayList<String>();
        for (int i=0; i<args.length; i++) argsList.add(args[i]);

		// -v (short version)
		if (hasCommandLineOption(argsList, "-v"))
		{
			System.out.println(Version.getVersion());
			System.exit(0);
		}

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


		// -debug for debugging
        Job.Mode runMode = Job.Mode.FULL_SCREEN;
		if (hasCommandLineOption(argsList, "-debug")) Job.setDebug(true);
        if (hasCommandLineOption(argsList, "-gilda")) Job.LOCALDEBUGFLAG = true;
        if (hasCommandLineOption(argsList, "-NOTHREADING")) Job.NOTHREADING = true;
		if (hasCommandLineOption(argsList, "-batch")) runMode = Job.Mode.BATCH;
        if (hasCommandLineOption(argsList, "-server")) {
            if (runMode != Job.Mode.FULL_SCREEN)
                System.out.println("Conflicting thread modes: " + runMode + " and " + Job.Mode.SERVER);
            runMode = Job.Mode.SERVER;
        }
        if (hasCommandLineOption(argsList, "-client")) {
            if (runMode != Job.Mode.FULL_SCREEN)
                System.out.println("Conflicting thread modes: " + runMode + " and " + Job.Mode.CLIENT);
            runMode = Job.Mode.CLIENT;
        }

		// see if there is a Mac OS/X interface
		Class osXClass = null;
		Method osXRegisterMethod = null, osXSetJobMethod = null;
        if (runMode != Job.Mode.BATCH)
        {
        	currentUI = new UserInterfaceMain();
            if (System.getProperty("os.name").toLowerCase().startsWith("mac"))
            {
                try
                {
                    osXClass = Class.forName("com.sun.electric.MacOSXInterface");

                    // find the necessary methods on the Mac OS/X class
                    try
                    {
                        osXRegisterMethod = osXClass.getMethod("registerMacOSXApplication", new Class[] {List.class});
                        osXSetJobMethod = osXClass.getMethod("setInitJob", new Class[] {Job.class});
                    } catch (NoSuchMethodException e)
                    {
                        osXRegisterMethod = osXSetJobMethod = null;
                    }
                    if (osXRegisterMethod != null)
                    {
                        try
                        {
                            osXRegisterMethod.invoke(osXClass, new Object[] {argsList});
                        } catch (Exception e)
                        {
                            System.out.println("Error initializing Mac OS/X interface");
                        }
                    }
                } catch (ClassNotFoundException e) {}
            }
    //		MacOSXInterface.registerMacOSXApplication(argsList);
        } else
        {
        	currentUI = new UserInterfaceDummy();
        }

		// -help
        if (hasCommandLineOption(argsList, "-help"))
		{
	        System.out.println("Usage (without plugins):");
	        System.out.println("\tjava -jar electric.jar [electric-options]");
	        System.out.println("Usage (with plugins):");
	        System.out.println("\tjava -classpath electric.jar<delim>{list of plugins} com.sun.electric.Launcher [electric-options]");
	        System.out.println("\t\twhere <delim> is OS-dependant separator (colon or semicolon)");
	        System.out.println("\nOptions:");
            System.out.println("\t-mdi: multiple document interface mode");
	        System.out.println("\t-sdi: single document interface mode");
	        System.out.println("\t-NOMINMEM: ignore minimum memory provided for JVM");
	        System.out.println("\t-s <script name>: bean shell script to execute");
	        System.out.println("\t-version: version information");
	        System.out.println("\t-v: brief version information");
	        System.out.println("\t-debug: debug mode. Extra information is available");
            System.out.println("\t-NOTHREADING: turn off Job threading.");
	        System.out.println("\t-batch: running in batch mode.");
	        System.out.println("\t-pulldowns: list all pulldown menus in Electric");
            System.out.println("\t-server: dump trace of snapshots");
            System.out.println("\t-client: replay trace of snapshots");
	        System.out.println("\t-help: this message");

			System.exit(0);
		}

        ActivityLogger.initialize(true, true, false);
        //runThreadStatusTimer();
        new EventProcessor();

		SplashWindow sw = null;

		if (runMode == Job.Mode.FULL_SCREEN) sw = new SplashWindow();

        boolean mdiMode = hasCommandLineOption(argsList, "-mdi");
        boolean sdiMode = hasCommandLineOption(argsList, "-sdi");
        TopLevel.Mode mode = null;
        if (mdiMode) mode = TopLevel.Mode.MDI;
        if (sdiMode) mode = TopLevel.Mode.SDI;
		TopLevel.OSInitialize(mode);

		if (hasCommandLineOption(argsList, "-pulldowns")) dumpPulldownMenus();

		// initialize database
        Job.setThreadMode(runMode);
		InitDatabase job = new InitDatabase(argsList, sw);
		if (osXRegisterMethod != null)
		{
			// tell the Mac OS/X system of the initialization job
			try
			{
				osXSetJobMethod.invoke(osXClass, new Object[] {job});
			} catch (Exception e)
			{
				System.out.println("Error initializing Mac OS/X interface");
			}
		}
//        MacOSXInterface.setInitJob(job);
        job.startJob();
	}

    private static class UserInterfaceDummy implements UserInterface
	{
		public EditWindow_ getCurrentEditWindow_() { return null; }
		public EditWindow_ needCurrentEditWindow_()
		{
			System.out.println("Batch mode Electric has no needed windows");
			return null; 
		}
        /** Get current cell from current library */
		public Cell getCurrentCell()
        {
			Library lib = Library.getCurrent();
			if (lib == null) return null;
			return lib.getCurCell();
        }
		public Cell needCurrentCell()
		{
            /** Current cell based on current library */
            Cell curCell = getCurrentCell();
            if (curCell == null)
            {
                System.out.println("There is no current cell for this operation.  To create one, use the 'New Cell' command from the 'Cell' menu.");
            }
            return curCell;
		}
		public void repaintAllEditWindows() {}
        public void adjustReferencePoint(Cell cell, double cX, double cY) {};
		public void alignToGrid(Point2D pt) {}
		public int getDefaultTextSize() { return 14; }
//		public Highlighter getHighlighter();
		public EditWindow_ displayCell(Cell cell) { return null; }

		public void wantToRedoErrorTree() { ; }
        public void wantToRedoJobTree() { ; }

        public void termLogging(final ErrorLogger logger, boolean explain) {;}

        /* Job related **/
        public void invokeLaterBusyCursor(final boolean state){;}
        public void setBusyCursor(boolean state) {;}

        /**
         * Method to return the error message associated with the current error.
         * Highlights associated graphics if "showhigh" is nonzero.  Fills "g1" and "g2"
         * with associated geometry modules (if nonzero).
         */
        public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, Geometric [] gPair)
        {
            // return the error message
            return log.getMessageString();
        }

        /**
         * Method to show an error message.
         * @param message the error message to show.
         * @param title the title of a dialog with the error message.
         */
        public void showErrorMessage(Object message, String title)
        {
        	System.out.println(message);
        }

        /**
         * Method to show an informational message.
         * @param message the message to show.
         * @param title the title of a dialog with the message.
         */
        public void showInformationMessage(Object message, String title)
        {
        	System.out.println(message);
        }

        /**
         * Method to show a message and ask for confirmation.
         * @param message the message to show.
         * @return true if "yes" was selected, false if "no" was selected.
         */
        public boolean confirmMessage(Object message) { return true; }

        /**
         * Method to ask for a choice among possibilities.
         * @param message the message to show.
         * @param title the title of the dialog with the query.
         * @param choices an array of choices to present, each in a button.
         * @param defaultChoice the default choice.
         * @return the index into the choices array that was selected.
         */
        public int askForChoice(Object message, String title, String [] choices, String defaultChoice)
        {
        	System.out.println(message + " CHOOSING " + defaultChoice);
        	for(int i=0; i<choices.length; i++) if (choices[i].equals(defaultChoice)) return i;
        	return 0;
        }

        /**
         * Method to ask for a line of text.
         * @param message the prompt message.
         * @param title the title of a dialog with the message.
         * @param def the default response.
         * @return the string (null if cancelled).
         */
        public String askForInput(Object message, String title, String def) { return def; }

        /** For Pref */
        public void restoreSavedBindings(boolean initialCall) {;}
        public void finishPrefReconcilation(String libName, List<Pref.Meaning> meaningsToReconcile)
        {
            Pref.finishPrefReconcilation(meaningsToReconcile);
        }

        /**
         * Method to import the preferences from an XML file.
         * Prompts the user and reads the file.
         */
        public void importPrefs() {;}

        /**
         * Method to export the preferences to an XML file.
         * Prompts the user and writes the file.
         */
        public void exportPrefs() {;}

        /** For TextWindow */
        public String [] getEditedText(Cell cell) { return null; }
        public void updateText(Cell cell, String [] strings) { ; }
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

	public static UserInterface getUserInterface() { return currentUI; }

	/** check if command line option 'option' present in 
     * command line args. If present, return true and remove if from the list.
     * Otherwise, return false.
     */
    private static boolean hasCommandLineOption(List<String> argsList, String option) 
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
    private static String getCommandLineOption(List<String> argsList, String option)
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
    public static void openCommandLineLibs(List<String> argsList)
    {
        List<URL> fileURLs = new ArrayList<URL>();
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
        new FileMenu.ReadInitialELIBs(fileURLs);
    }

    /**
     * Method to dump the pulldown menus in indented style.
     */
    private static void dumpPulldownMenus()
    {
		Date now = new Date();
    	System.out.println("Pulldown menus in Electric as of " + TextUtils.formatDate(now));
        TopLevel top = (TopLevel)TopLevel.getCurrentJFrame();
    	MenuBar menuBar = top.getTheMenuBar();
        for (int i=0; i<menuBar.getMenuCount(); i++)
        {
            Menu menu = (Menu)menuBar.getMenu(i);
            printIndented("\n" + menu.getText() + ":", 0);
            addMenu(menu, 1);
        }
    }
    private static void printIndented(String str, int depth)
    {
    	for(int i=0; i<depth*3; i++) System.out.print(" ");
    	System.out.println(str);
    	
    }
    private static void addMenu(Menu menu, int depth)
    {
        for (int i=0; i<menu.getItemCount(); i++)
        {
            JMenuItem menuItem = menu.getItem(i);
            if (menuItem == null) { printIndented("----------", depth); continue; }
            printIndented(menuItem.getText(), depth);
            if (menuItem instanceof JMenu)
                addMenu((Menu)menuItem, depth+1);              // recurse
        }
    }

	/**
	 * Class to init all technologies.
	 */
	private static class InitDatabase extends Job
	{
		List<String> argsList;
        String beanShellScript;
		transient SplashWindow sw;

		public InitDatabase() {}

		protected InitDatabase(List<String> argsList, SplashWindow sw)
		{
			super("Init database", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.argsList = argsList;
			this.sw = sw;
			if (hasCommandLineOption(argsList, "-NOMINMEM")) {
				// do nothing, just consume option: handled in Launcher
			}
			beanShellScript = getCommandLineOption(argsList, "-s");
			//startJob();
		}

		public boolean doIt() throws JobException
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
			} finally {
				Undo.changesQuiet(false);
			}

			openCommandLineLibs(argsList);

            return true;
		}
        
        public void terminateIt(Throwable jobException) {
            if (jobException != null) return;
            
            // finish initializing the GUI
            if (!BATCHMODE)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        // remove the splash screen
                        if (sw != null) sw.removeNotify();
                        TopLevel.InitializeWindows();
                        WindowFrame.wantToOpenCurrentLibrary(true);
                        // run script
                        if (beanShellScript != null) EvalJavaBsh.runScript(beanShellScript);
                    }
                });
            }
            else
            {
               // run script
               if (beanShellScript != null) EvalJavaBsh.runScript(beanShellScript);
            }
        }
	}
    
    /**
	 * Method to return the amount of memory being used by Electric.
	 * Calls garbage collection and delays to allow completion, so the method is SLOW.
	 * @return the number of bytes being used by Electric.
	 */
	public static long getMemoryUsage()
	{
		collectGarbage();
		collectGarbage();
		long totalMemory = Runtime.getRuntime().totalMemory();

		collectGarbage();
		collectGarbage();
		long freeMemory = Runtime.getRuntime().freeMemory();
		
		return (totalMemory - freeMemory);
	}

	private static void collectGarbage()
	{
		try
		{
			System.gc();
			Thread.sleep(100);
			System.runFinalization();
			Thread.sleep(100);
		} catch (InterruptedException ex)
		{
			ex.printStackTrace();
		}
	}

	/**
     * Places a custom event processor on the event queue in order to
     * catch all exceptions generated by event processing.
     */
    private static class EventProcessor extends EventQueue
    {
		private EventProcessor() {
            if (!Job.BATCHMODE)
            {
            Toolkit kit = Toolkit.getDefaultToolkit();
            kit.getSystemEventQueue().push(this);
            }
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

//    private static void runThreadStatusTimer() {
//        int delay = 1000*60*10; // milliseconds
//        Timer timer = new Timer(delay, new ThreadStatusTask());
//        timer.start();
//    }

    private static class ThreadStatusTask implements ActionListener {
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

}
