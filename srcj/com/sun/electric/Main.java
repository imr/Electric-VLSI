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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.AbstractUserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.menus.FileMenu;

import java.awt.geom.Point2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
			System.out.println("\t"+Version.getCopyrightInformation());
			System.out.println("\t"+Version.getWarrantyInformation());
			System.exit(0);
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
            System.out.println("\t-threads <numThreads>: recommended size of thread pool for Job execution.");
	        System.out.println("\t-batch: running in batch mode.");
//	        System.out.println("\t-pulldowns: list all pulldown menus in Electric"); moved to DebugMenus DN
            System.out.println("\t-server: dump trace of snapshots");
            System.out.println("\t-client <machine name>: replay trace of snapshots");
	        System.out.println("\t-help: this message");

			System.exit(0);
		}

        ActivityLogger.initialize(true, true, true/*false*/);

		// -debug for debugging
        Job.Mode runMode = Job.Mode.FULL_SCREEN;
		if (hasCommandLineOption(argsList, "-debug")) Job.setDebug(true);
        if (hasCommandLineOption(argsList, "-extraDebug")) Job.LOCALDEBUGFLAG = true;
        String numThreadsString = getCommandLineOption(argsList, "-threads");
        int numThreads = 0 ;
        if (numThreadsString != null) {
            numThreads = TextUtils.atoi(numThreadsString);
            if (numThreads <= 0) {
                System.out.println("Invalid option -threads " + numThreadsString);
            }
        }
        
		if (hasCommandLineOption(argsList, "-batch")) runMode = Job.Mode.BATCH;
        if (hasCommandLineOption(argsList, "-server")) {
            if (runMode != Job.Mode.FULL_SCREEN)
                System.out.println("Conflicting thread modes: " + runMode + " and " + Job.Mode.SERVER);
            runMode = Job.Mode.SERVER;
        }
        String serverMachineName = getCommandLineOption(argsList, "-client");
        if (serverMachineName != null) {
            if (runMode != Job.Mode.FULL_SCREEN)
                System.out.println("Conflicting thread modes: " + runMode + " and " + Job.Mode.CLIENT);
            runMode = Job.Mode.CLIENT;
        }

        boolean mdiMode = hasCommandLineOption(argsList, "-mdi");
        boolean sdiMode = hasCommandLineOption(argsList, "-sdi");
        UserInterfaceMain.Mode mode = null;
        if (mdiMode) mode = UserInterfaceMain.Mode.MDI;
        if (sdiMode) mode = UserInterfaceMain.Mode.SDI;
        
        AbstractUserInterface ui;
        if (runMode == Job.Mode.FULL_SCREEN || runMode == Job.Mode.CLIENT)
            ui = new UserInterfaceMain(argsList, mode, runMode == Job.Mode.FULL_SCREEN);
        else
            ui = new UserInterfaceDummy();
        Job.setThreadMode(runMode, ui);

		// initialize database
        EDatabase.serverDatabase();
		InitDatabase job = new InitDatabase(argsList);
        Job.initJobManager(numThreads, job, mode, serverMachineName);
	}

    private static class UserInterfaceDummy extends AbstractUserInterface
	{
        private long oldPct;
        
        /**
         * Method to start progress bar
         */
        public void startProgressDialog(String type, String filePath)
        {
            System.out.println("Starting Progress Dialog");
        }

        /**
         * Method to stop the progress bar
         */
        public void stopProgressDialog()
        {
            System.out.println("Stopping Progress Dialog");
        }

        /**
         * Method to update the progress bar
         */
        public void setProgressValue(long pct)
        {
            if (pct == oldPct) return;
            System.out.print(".");
            oldPct = pct;
        }

        /**
         * Method to set a text message in the progress dialog.
         * @param message
         */
        public void setProgressNote(String message)
        {
            System.out.println("Progress Note: '" + message + "'");
        }

        /**
         * Method to get text message in the progress dialgo.
         * @return
         */
        public String getProgressNote()
        {
            System.out.println("Batch mode Electric has no getProgressNote");
            return null;
        }

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
		/**
		 * Method to get the current Cell in a given Library.
		 * @param lib the library to query.
		 * @return the current Cell in the Library.
		 * @return the current cell in the library; null if there is no current Cell.
		 */
		public Cell getCurrentCell(Library lib)
		{
			return lib.getCurCell();
		}

		/**
		 * Method to set the current Cell in a Library.
		 * @param lib the library in which to set a current cell.
		 * @param curCell the new current Cell in the Library (can be null).
		 */
		public void setCurrentCell(Library lib, Cell curCell)
		{
			lib.setCurCell(curCell);
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
        public void loadComponentMenuForTechnology()
        {
             System.out.println("Batch mode Electric has no loadComponentMenuForTechnology");
        }
        public void adjustReferencePoint(Cell cell, double cX, double cY) {};
		public void alignToGrid(Point2D pt) {}
		public int getDefaultTextSize() { return 14; }
//		public Highlighter getHighlighter();
		public EditWindow_ displayCell(Cell cell) { return null; }

        public void termLogging(final ErrorLogger logger, boolean explain, boolean terminate) {
            System.out.println(logger.getInfo());
        }

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
        public int askForChoice(String message, String title, String [] choices, String defaultChoice)
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
	}

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
            User.setWorkingDirectory(TextUtils.getFilePath(url));
            // setting database path for future references
            FileType.setDatabaseGroupPath(User.getWorkingDirectory());
        }

        // open any libraries but only when there is at least one
        new FileMenu.ReadInitialELIBs(fileURLs);
    }

	/**
	 * Class to init all technologies.
	 */
	private static class InitDatabase extends Job
	{
		List<String> argsList;
        String beanShellScript;

		protected InitDatabase(List<String> argsList)
		{
			super("Init database", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.argsList = argsList;
			if (hasCommandLineOption(argsList, "-NOMINMEM")) {
				// do nothing, just consume option: handled in Launcher
			}
			beanShellScript = getCommandLineOption(argsList, "-s");
			//startJob();
		}

		public boolean doIt() throws JobException
		{
//			try {
//				Undo.changesQuiet(true);

				// initialize all of the technologies
				Technology.initAllTechnologies();

				// initialize all of the tools
				Tool.initAllTools();

				// initialize the constraint system
//				Layout con = Layout.getConstraint();
//				Constraints.setCurrent(con);
//			} finally {
//				Undo.changesQuiet(false);
//			}

            // open no name library first
            Input.changesQuiet(true);
            Library mainLib = Library.newInstance("noname", null);
            if (mainLib == null) return false;
            mainLib.setCurrent();
            Input.changesQuiet(false);

			openCommandLineLibs(argsList);
            if (beanShellScript != null)
                EvalJavaBsh.runScript(beanShellScript);
                
            return true;
		}
        
        public void terminateOK() {
            Job.getExtendedUserInterface().finishInitialization();
        }
        
        public void terminateFail(Throwable jobException) {
            System.out.println("Initialization failed");
            System.exit(1);
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

}
