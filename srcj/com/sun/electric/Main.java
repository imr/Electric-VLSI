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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Environment;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.ImmutableLibrary;
import com.sun.electric.database.LibraryBackup;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.AbstractUserInterface;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.MessagesStream;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.menus.FileMenu;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
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

        if (hasCommandLineOption(argsList, "-pipeserver")) {
            Job.pipeServer(numThreads);
            MessagesStream.getMessagesStream();
            return;
        }

        ActivityLogger.initialize(true, true, true/*false*/);

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

        UserInterfaceMain.Mode mode = null;
        int defMode = StartupPrefs.getDisplayStyle();
        if (defMode == 1) mode = UserInterfaceMain.Mode.MDI; else
            if (defMode == 2) mode = UserInterfaceMain.Mode.SDI;
        if (hasCommandLineOption(argsList, "-mdi")) mode = UserInterfaceMain.Mode.MDI;
        if (hasCommandLineOption(argsList, "-sdi")) mode = UserInterfaceMain.Mode.SDI;

        AbstractUserInterface ui;
        if (runMode == Job.Mode.FULL_SCREEN || runMode == Job.Mode.CLIENT)
            ui = new UserInterfaceMain(argsList, mode, runMode == Job.Mode.FULL_SCREEN);
        else
            ui = new UserInterfaceDummy();
        Job.setThreadMode(runMode, ui);
        MessagesStream.getMessagesStream();

		// initialize database
        EDatabase.theDatabase = new EDatabase(makeInitialSnapshot());
		InitDatabase job = new InitDatabase(argsList);
        Job.initJobManager(numThreads, job, mode, serverMachineName);
	}

    private static Snapshot makeInitialSnapshot() {
        Environment env = Technology.makeInitialEnvironment();
        TechPool techPool = env.techPool;
        CellId clipCellId = Clipboard.clipCellId;
        LibraryBackup[] libBackupsArray = new LibraryBackup[] {
            new LibraryBackup(ImmutableLibrary.newInstance(clipCellId.libId, null, null).withFlags(Library.HIDDENLIBRARY), false, new LibId[0])
        };
        CellBackup[] cellBackupsArray = new CellBackup[] {
            CellBackup.newInstance(ImmutableCell.newInstance(clipCellId, 0).withTechId(techPool.getGeneric().getId()), techPool)
        };
        ERectangle[] cellBoundsArray = new ERectangle[] {
            ERectangle.fromGrid(0, 0, 0, 0)
        };
        return new Snapshot(env.techPool.idManager).with(null, env, cellBackupsArray, cellBoundsArray, libBackupsArray);
    }

    public static class UserInterfaceDummy extends AbstractUserInterface
	{
        private PrintStream stdout = System.out;

        public void startProgressDialog(String type, String filePath) {}
        public void stopProgressDialog() {}
        public void setProgressValue(long pct) {}
        public void setProgressNote(String message) {}
        public String getProgressNote() { return null; }

    	public EDatabase getDatabase() {
            return EDatabase.theDatabase;
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
            throw new IllegalStateException("Batch mode Electric has no current Cell");
        }

		public Cell needCurrentCell()
		{
            throw new IllegalStateException("Batch mode Electric has no current Cell");
		}
		public void repaintAllWindows() {}

        public void adjustReferencePoint(Cell cell, double cX, double cY) {};
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
        public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, Geometric[] gPair, int position)
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

        private PrintWriter printWriter = null;

        /**
         * Method print a message.
         * @param message the message to show.
         * @param newLine add new line after the message
         */
        public void printMessage(String message, boolean newLine) {
            if (newLine) {
                stdout.println(message);
                if (printWriter != null)
                    printWriter.println(message);
            } else {
                stdout.print(message);
                if (printWriter != null)
                    printWriter.print(message);
            }
        }

        /**
         * Method to start saving messages.
         * @param filePath file to save
         */
        public void saveMessages(final String filePath) {
            try
            {
                if (printWriter != null) {
                    printWriter.close();
                    printWriter = null;
                }
                if (filePath == null) return;
                printWriter = new PrintWriter(new BufferedWriter(new FileWriter(filePath)));
            } catch (IOException e)
            {
                System.err.println("Error creating " + filePath);
                System.out.println("Error creating " + filePath);
                return;
            }

            System.out.println("Messages will be saved to " + filePath);
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

        @Override
        protected void consume(EJobEvent e) {
            printMessage("Job " + e.ejob, true);
    //        writeEJobEvent(e.ejob, e.newState, e.timeStamp);
        }

        @Override
        protected void consume(PrintEvent e) {
            printMessage(e.s, false);
        }

        @Override
        protected void consume(JobQueueEvent e) {
            printMessage("JobQueue: ", false);
            for (Job.Inform jobInfo: e.jobQueue)
                printMessage(" " + jobInfo, false);
            printMessage("", true);
        }

        @Override
        protected void addEvent(Client.ServerEvent serverEvent) {
            serverEvent.run();
        }
	}

	/** check if command line option 'option' present in
     * command line args. If present, return true and remove if from the list.
     * Otherwise, return false.
     */
    private static boolean hasCommandLineOption(List<String> argsList, String option)
    {
        for (int i=0; i<argsList.size(); i++) {
            if (argsList.get(i).equals(option)) {
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
            if (argsList.get(i).equals(option)) {
                argsList.remove(i); // note that this shifts objects in arraylist
                // check if next string valid (i.e. no dash)
                if (argsList.get(i).startsWith("-")) {
                    System.out.println("Bad command line option: "+ option +" "+ argsList.get(i+1));
                    return null;
                }
                return argsList.remove(i);
            }
        }
        return null;
    }

    /** open any libraries specified on the command line.  This method should be
     * called after any valid options have been parsed out
     */
    public static void openCommandLineLibs(List<String> argsList)
    {
        for (int i=0; i<argsList.size(); i++) {
            String arg = argsList.get(i);
            if (arg.startsWith("-")) {
                System.out.println("Command line option "+arg+" not understood, ignoring.");
                continue;
            }
			URL url = TextUtils.makeURLToFile(arg);
            if (url == null) continue;
            User.setWorkingDirectory(TextUtils.getFilePath(url));
            // setting database path for future references
            FileType.setDatabaseGroupPath(User.getWorkingDirectory());
            FileMenu.openLibraryCommand(url);
        }
    }

	/**
	 * Class to init all technologies.
	 */
	private static class InitDatabase extends Job
	{
		List<String> argsList;
        String beanShellScript;
        private Library mainLib;

		protected InitDatabase(List<String> argsL)
		{
			super("Init database", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.argsList = argsL;
			if (hasCommandLineOption(argsList, "-NOMINMEM")) {
				// do nothing, just consume option: handled in Launcher
			}
			beanShellScript = getCommandLineOption(argsList, "-s");
			//startJob();
		}

        @Override
		public boolean doIt() throws JobException
		{
//			try {
//				Undo.changesQuiet(true);

				// initialize all of the technologies
				Technology.initAllTechnologies();
				// initialize all of the tools
				Tool.initAllTools();

                Pref.lockCreation();

				// initialize the constraint system
//				Layout con = Layout.getConstraint();
//				Constraints.setCurrent(con);
//			} finally {
//				Undo.changesQuiet(false);
//			}

            // open no name library first
//            Input.changesQuiet(true);
            mainLib = Library.newInstance("noname", null);
            if (mainLib == null) return false;
            fieldVariableChanged("mainLib");
            mainLib.clearChanged();
//            mainLib.setCurrent();
//            Input.changesQuiet(false);

            if (Job.BATCHMODE) {
                EditingPreferences.setThreadEditingPreferences(new EditingPreferences(true, getTechPool()));
                if (beanShellScript != null)
                    EvalJavaBsh.runScript(beanShellScript);
            }
            return true;
		}

        @Override
        public void terminateOK() {
            User.setCurrentLibrary(mainLib);
            Environment.setThreadEnvironment(EDatabase.clientDatabase().getEnvironment());
            Job.getExtendedUserInterface().finishInitialization();
			openCommandLineLibs(argsList);
            if (beanShellScript != null)
                EvalJavaBsh.runScript(beanShellScript);
        }

        @Override
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
