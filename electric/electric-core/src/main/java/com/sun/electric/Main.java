/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Main.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.Icon;
import javax.swing.JApplet;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.AbstractUserInterface;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.UserInterfaceInitial;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.SimulationData;
import com.sun.electric.tool.lang.EvalJavaBsh;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.Clipboard;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.MessagesStream;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.UserInterfaceMain;
import com.sun.electric.tool.user.menus.EMenuBar;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.ui.MessagesWindow;
import com.sun.electric.tool.user.ui.StatusBar;
import com.sun.electric.tool.user.ui.ToolBar;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool.PoolWorkerStrategyFactory;
import com.sun.electric.tool.util.concurrent.utils.ElapseTimer;
import com.sun.electric.util.PropertiesUtils;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.concurrent.ElectricWorkerStrategy;
import com.sun.electric.util.config.Configuration;
import com.sun.electric.util.test.TestByReflection;

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
 * <P> <CODE>         -server: dump strace of snapshots</CODE>
 * <P> <CODE>         -help: this message </CODE>
 * <P> <P>
 * See manual for more instructions.
 */
public final class Main extends JApplet
{
	private static final long serialVersionUID = 1L;

	/**
     * Mode of Job manager
     */
    private static enum Mode {
        /** Thread-safe full screen run. */                                    FULL_SCREEN_SAFE,
        /** JonG: "I think batch mode implies 'no GUI', and nothing more." */  BATCH,
        /** Server side. */                                                    SERVER,
        /** Client side. */                                                    CLIENT;
    }

    private static final Mode DEFAULT_MODE = Mode.FULL_SCREEN_SAFE;

    private static Mode runMode;
    private static Set<Window> modelessDialogs = new HashSet<Window>();
    static Main toplevel = null;
    public static JDesktopPane desktop = null;
    static StatusBar sb = null;
    static Dimension scrnSize;
    static Dimension appSize;
    static MessagesWindow messagesWindow;
    static EMenuBar.Instance menuBar;
    static ToolBar toolBar;
    static int doubleClickDelay;
    static Cursor cursor;
    static boolean busyCursorOn = false;
    
    /** JonG: "I think batch mode implies 'no GUI', and nothing more." */
    public static boolean isBatch() {
        return runMode == Mode.BATCH;
    }

	/**
	 * The main entry point of Electric.
	 * @param args the arguments to the program.
	 */
    public static void main(String[] args) {
    	JFrame appframe = new JFrame("Christa");
    	Main applet = new Main();
    	
    	appframe.getContentPane().add(applet, BorderLayout.CENTER);
    	
    	applet.init();
    	applet.start();
    	
    	appframe.setSize(1024,800);
    	appframe.setVisible(true);
    }
    
	public void init()
	{
		// convert args to array list
        List<String> argsList = new ArrayList<String>();
        //for (int i=0; i<args.length; i++) argsList.add(args[i]);
/**
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
            System.out.println("\t-logging <filePath>: log server events in a binary file");
            System.out.println("\t-socket <socket>: socket port for client/server interaction");
	        System.out.println("\t-batch: batch mode implies 'no GUI', and nothing more");
            System.out.println("\t-server: dump trace of snapshots");
            System.out.println("\t-client <machine name>: replay trace of snapshots");
	        System.out.println("\t-help: this message");

			System.exit(0);
		}
**/
        
        appSize = getSize();
        Toolkit tk = Toolkit.getDefaultToolkit();
        scrnSize = tk.getScreenSize();
        
		// -debug for debugging
        runMode = DEFAULT_MODE;
        List<String> pipeOptions = new ArrayList<String>();
		if (hasCommandLineOption(argsList, "-debug")) {
            pipeOptions.add(" -debug");
            Job.setDebug(true);
        }
        if (hasCommandLineOption(argsList, "-extraDebug")) {
            pipeOptions.add("-extraDebug");
            Job.LOCALDEBUGFLAG = true;
        }
        String numThreadsString = getCommandLineOption(argsList, "-threads");
        int numThreads = 0 ;
        if (numThreadsString != null) {
            numThreads = TextUtils.atoi(numThreadsString);
            if (numThreads > 0) {
                pipeOptions.add("-threads");
                pipeOptions.add(String.valueOf(numThreads));
            }  else
                System.out.println("Invalid option -threads " + numThreadsString);
        }
        String loggingFilePath = getCommandLineOption(argsList, "-logging");
        if (loggingFilePath != null) {
            pipeOptions.add("-logging");
            pipeOptions.add(loggingFilePath);
        }
        String socketString = getCommandLineOption(argsList, "-socket");
        int socketPort = 0;
        if (socketString != null) {
            socketPort = TextUtils.atoi(socketString);
            if (socketPort > 0) {
                pipeOptions.add("-socket");
                pipeOptions.add(String.valueOf(socketPort));
            }  else
                System.out.println("Invalid option -socket " + socketString);
        }
        hasCommandLineOption(argsList, "-NOMINMEM"); // do nothing, just consume option: handled in Launcher

        // The server runs in subprocess
        if (hasCommandLineOption(argsList, "-pipeserver")) {
            ActivityLogger.initialize("electricserver", true, true, true/*false*/, true, false);
            Job.pipeServer(numThreads, loggingFilePath, socketPort);
            return;
        }

        ActivityLogger.initialize("electric", true, true, true/*false*/, User.isEnableLog(), User.isMultipleLog());

        if (hasCommandLineOption(argsList, "-batch")) {
            //System.setProperty("java.awt.headless", "true");
            runMode = Mode.BATCH;
        }
        if (hasCommandLineOption(argsList, "-server")) {
            if (runMode != DEFAULT_MODE)
                System.out.println("Conflicting thread modes: " + runMode + " and " + Mode.SERVER);
            runMode = Mode.SERVER;
        }
        String serverMachineName = getCommandLineOption(argsList, "-client");
        if (serverMachineName != null) {
            if (runMode != DEFAULT_MODE)
                System.out.println("Conflicting thread modes: " + runMode + " and " + Mode.CLIENT);
            runMode = Mode.CLIENT;
        }
        if (hasCommandLineOption(argsList, "-pipe")) {
            if (runMode != DEFAULT_MODE)
                System.out.println("Conflicting thread modes: " + runMode + " and " + Mode.CLIENT);
             runMode = Mode.CLIENT;
        }
        if (hasCommandLineOption(argsList, "-pipedebug")) {
            if (runMode != DEFAULT_MODE)
                System.out.println("Conflicting thread modes: " + runMode + " and " + Mode.CLIENT);
            runMode = Mode.CLIENT;
        }

        UserInterfaceMain.Mode mode = null;
        if (hasCommandLineOption(argsList, "-mdi")) mode = UserInterfaceMain.Mode.MDI;
        if (hasCommandLineOption(argsList, "-sdi")) mode = UserInterfaceMain.Mode.SDI;

        desktop = new JDesktopPane();
        desktop.setVisible(true);
        getContentPane().add(desktop);
        
        AbstractUserInterface ui;
        if (runMode == Mode.FULL_SCREEN_SAFE || runMode == Mode.CLIENT)
            ui = new UserInterfaceMain(argsList, mode, this);
        else
            ui = new UserInterfaceDummy();
        MessagesStream.getMessagesStream();

		// initialize database
        TextDescriptor.cacheSize();
        Tool.initAllTools();
        Pref.lockCreation();
        EDatabase database = new EDatabase(IdManager.stdIdManager.getInitialSnapshot(), "clientDB");
        EDatabase.setClientDatabase(database);
        Job.setUserInterface(new UserInterfaceInitial(database));
        InitDatabase job = new InitDatabase(argsList);
        EDatabase.setServerDatabase(new EDatabase(IdManager.stdIdManager.getInitialSnapshot(), "serverDB"));
        EDatabase.setCheckExamine();
        Job.initJobManager(numThreads, loggingFilePath, socketPort, ui, job);
   	}
	
	public void start(){
		
	}
	
	public void stop(){
		
	}

	public static void InitializeMessagesWindow() {
        messagesWindow = new MessagesWindow(appSize);
	}

    private static Process invokePipeserver(List<String> electricOptions, boolean withDebugger) throws IOException {
        List<String> javaOptions = new ArrayList<String>();
        javaOptions.add("-Xss2m");
		int maxMemWanted = StartupPrefs.getMemorySize();
        javaOptions.add("-Xmx" + maxMemWanted + "m");
        long maxPermWanted = StartupPrefs.getPermSpace();
        if (maxPermWanted > 0)
            javaOptions.add("-XX:MaxPermSize=" + maxPermWanted + "m");
        if (withDebugger) {
            javaOptions.add("-Xdebug");
            javaOptions.add("-Xrunjdwp:transport=dt_socket,server=n,address=localhost:35856");
        }
        return invokePipeserver(javaOptions, electricOptions);
    }

    public static class UserInterfaceDummy extends AbstractUserInterface
	{
        public static final PrintStream stdout = System.out;

        public UserInterfaceDummy() {
        }

        public void startProgressDialog(String type, String filePath) {}
        public void stopProgressDialog() {}
        public void setProgressValue(int pct) {}
        public void setProgressNote(String message) {}
        public String getProgressNote() { return null; }

    	public EDatabase getDatabase() {
            return EDatabase.clientDatabase();
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
         * Highlights associated graphics if "showhigh" is nonzero.
         */
        public String reportLog(ErrorLogger.MessageLog log, boolean showhigh, boolean separateWindow, int position)
        {
            // return the error message
            return log.getMessageString();
        }

        /**
         * Method to show an error message.
         * @param message the error message to show.
         * @param title the title of a dialog with the error message.
         */
        public void showErrorMessage(String message, String title)
        {
        	System.out.println(message);
        }

        /**
         * Method to show an error message.
         * @param message the error message to show.
         * @param title the title of a dialog with the error message.
         */
        public void showErrorMessage(String[] message, String title)
        {
        	System.out.println(message);
        }

        /**
         * Method to show an informational message.
         * @param message the message to show.
         * @param title the title of a dialog with the message.
         */
        public void showInformationMessage(String message, String title)
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
        protected void terminateJob(Job.Key jobKey, String jobName, Tool tool,
            Job.Type jobType, byte[] serializedJob,
            boolean doItOk, byte[] serializedResult, Snapshot newSnapshot) {
            printMessage("Job " + jobKey, true);
            if (!jobType.isExamine()) {
                endChanging();
            }
        }

        @Override
        protected void showJobQueue(Job.Inform[] jobQueue) {
            printMessage("JobQueue: ", false);
            for (Job.Inform jobInfo: jobQueue)
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
            if (!isBatch()) User.setWorkingDirectory(TextUtils.getFilePath(url));
            // setting database path for future references
            FileType.setDatabaseGroupPath(User.getWorkingDirectory());
            if (arg.indexOf('.')!=-1 && SimulationData.isKnownSimulationFormatExtension(arg.substring(arg.lastIndexOf('.')+1))) {
                SimulationData.plot(null, url, null);
            } else {
                FileMenu.openLibraryCommand(url);
            }
        }
    }

	/**
	 * Class to init all technologies.
	 */
	private static class InitDatabase extends Job
	{
        private Map<String,Object> paramValuesByXmlPath = Technology.getParamValuesByXmlPath();
		private List<String> argsList;
        private Library mainLib;

		private InitDatabase(List<String> argsList)
		{
			super("Init database", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.argsList = argsList;
		}

        @Override
		public boolean doIt() throws JobException
		{
            //System.out.println("InitDatabase");
            // initialize all of the technologies
            Technology.initPreinstalledTechnologies(getDatabase(), paramValuesByXmlPath);

            // open no name library first
            Library clipLib = Library.newInstance(Clipboard.CLIPBOARD_LIBRAY_NAME, null);
            clipLib.setHidden();
            Cell clipCell = Cell.newInstance(clipLib, Clipboard.CLIPBOARD_CELL_NAME);
            assert clipCell.getId().cellIndex == Clipboard.CLIPBOARD_CELL_INDEX;
            clipCell.setTechnology(getTechPool().getGeneric());

            mainLib = Library.newInstance("noname", null);
            if (mainLib == null) return false;
            fieldVariableChanged("mainLib");
            mainLib.clearChanged();
            return true;
		}

        @Override
        public void terminateOK() {
            new InitProjectSettings(argsList).startJobOnMyResult();
            User.setCurrentLibrary(mainLib);
        }

        @Override
        public void terminateFail(Throwable jobException) {
            System.out.println("Initialization failed");
            System.exit(1);
        }
	}

	/**
	 * Class to init project preferences.
	 */
	private static class InitProjectSettings extends Job
	{
        private Setting.SettingChangeBatch changeBatch = new Setting.SettingChangeBatch();
		List<String> argsList;

		private InitProjectSettings(List<String> argsList)
		{
			super("Init project preferences", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            Preferences prefRoot = Pref.getPrefRoot();
            for (Map.Entry<Setting,Object> e: getDatabase().getSettings().entrySet()) {
                Setting setting = e.getKey();
                Object value = setting.getValueFromPreferences(prefRoot);
                if (value.equals(e.getValue())) continue;
                changeBatch.add(setting, value);
            }
            this.argsList = argsList;
		}

        @Override
		public boolean doIt() throws JobException
		{
            getDatabase().implementSettingChanges(changeBatch);
            return true;
		}

        @Override
        public void terminateOK() {
            Job.getExtendedUserInterface().finishInitialization();
			String beanShellScript = getCommandLineOption(argsList, "-s");
            openCommandLineLibs(argsList);
            if (beanShellScript != null)
                EvalJavaBsh.runScript(beanShellScript);
        }
	}

	/**
	 * Class to init project preferences.
	 */
	private static class InitClient extends Job
	{
        private Setting.SettingChangeBatch changeBatch = new Setting.SettingChangeBatch();
		List<String> argsList;

		private InitClient(List<String> argsList)
		{
			super("Init project preferences", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.argsList = argsList;
		}

        @Override
		public boolean doIt() throws JobException
		{
            return true;
		}

        @Override
        public void terminateOK() {
            Job.getExtendedUserInterface().finishInitialization();
			String beanShellScript = getCommandLineOption(argsList, "-s");
            openCommandLineLibs(argsList);
            if (beanShellScript != null)
                EvalJavaBsh.runScript(beanShellScript);
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
	
	    private static final boolean enableAssertions = true;
	    private static final Logger logger = Logger.getLogger(Main.class.getName());

	    private static final String[] propertiesToCopy = { "user.home" };

	    private static String additionalFolder = "additional";
	    private static String configFile = "econfig.xml";

	    private static String[] getConfig(String[] args) {
	        for (String arg : args) {
	            if (arg.startsWith("-config=")) {
	                configFile = arg.replaceAll("-config=", "");
	                args = removeArg(args, arg);
	                return args;
	            }
	        }
	        return args;
	    }

	    private static String[] removeArg(String[] args, String option) {
	        List<String> tmp = new ArrayList<String>();
	        Collections.addAll(tmp, args);
	        tmp.remove(option);
	        return tmp.toArray(new String[tmp.size()]);
	    }

	    @TestByReflection(testMethodName = "getAdditionalFolder")
	    private static String[] getAdditionalFolder(String[] args) {
	        for (String arg : args) {
	            if (arg.startsWith("-additionalfolder=")) {
	                additionalFolder = arg.replaceAll("-additionalfolder=", "");
	                args = removeArg(args, arg);
	                return args;
	            }
	        }
	        return args;
	    }

	    /**
	     * Method to return a String that gives the path to the Electric JAR file.
	     * If the path has spaces in it, it is quoted.
	     * 
	     * @return the path to the Electric JAR file.
	     */
	public static String getJarLocation() {
	    String jarPath = System.getProperty("java.class.path", ".");
	    if (jarPath.indexOf(' ') >= 0)
	        jarPath = "\"" + jarPath + "\"";
	    return jarPath;
	}

	    private static int getUserInt(String key, int def) {
	        return Preferences.userNodeForPackage(Main.class).node(StartupPrefs.USER_NODE).getInt(key, def);
	    }

	    private static boolean invokeRegression(String[] args) {
	        List<String> javaOptions = new ArrayList<String>();
	        List<String> electricOptions = new ArrayList<String>();
	        electricOptions.add("-debug");
	        int regressionPos = 0;
	        if (args[0].equals("-threads")) {
	            regressionPos = 2;
	            electricOptions.add("-threads");
	            electricOptions.add(args[1]);
	        } else if (args[0].equals("-logging")) {
	            regressionPos = 2;
	            electricOptions.add("-logging");
	            electricOptions.add(args[1]);
	        } else if (args[0].equals("-debug")) {
	            regressionPos = 1;
	            // no need of adding the -debug flag
	        }
	        String script = args[regressionPos + 1];

	        for (int i = regressionPos + 2; i < args.length; i++)
	            javaOptions.add(args[i]);
	        Process process = null;
	        try {
	            process = invokePipeserver(javaOptions, electricOptions);
	        } catch (java.io.IOException e) {
	            logger.log(Level.SEVERE, "Failure starting server subprocess", e);
	            return false;
	        }
	        initClasspath(true);
	        boolean result = ((Boolean) callByReflection(ClassLoader.getSystemClassLoader(),
	                "com.sun.electric.tool.Regression", "runScript", new Class[] { Process.class, String.class },
	                null, new Object[] { process, script })).booleanValue();
	        return result;
	    }

	public static Process invokePipeserver(List<String> javaOptions, List<String> electricOptions)
	        throws IOException {
	    List<String> procArgs = new ArrayList<String>();
	    String program = "java";
	    String javaHome = System.getProperty("java.home");
	    if (javaHome != null)
	        program = javaHome + File.separator + "bin" + File.separator + program;

	    procArgs.add(program);
	    procArgs.add("-ea");
	    procArgs.add("-Djava.util.prefs.PreferencesFactory=com.sun.electric.database.text.EmptyPreferencesFactory");
	    procArgs.addAll(javaOptions);
	    procArgs.add("-cp");
	    procArgs.add(getJarLocation());
	    procArgs.add("com.sun.electric.Launcher");
	    procArgs.addAll(electricOptions);
	    procArgs.add("-pipeserver");
	    String command = "";
	    for (String arg : procArgs)
	        command += " " + arg;
	    logger.log(Level.INFO, "exec: {0}", new Object[] { command });

	    return new ProcessBuilder(procArgs).start();
	}

	    private static ClassLoader pluginClassLoader;

//	    public static Class classFromPlugins(String className) throws ClassNotFoundException {
//	        return pluginClassLoader.loadClass(className);
//	    }

//	    private static void loadAndRunMain(String[] args, boolean loadDependencies) {
//	        args = getAdditionalFolder(args);
//	        args = getConfig(args);
//
//	        Configuration.setConfigName(configFile);
//
//	        initClasspath(loadDependencies);
//	        callByReflection(pluginClassLoader, "com.sun.electric.Main", "main", new Class[] { String[].class },
//	                null, new Object[] { args });
//	    }

	    private static void initClasspath(boolean loadDependencies) {
	        ClassLoader launcherLoader = Main.class.getClassLoader();
	        pluginClassLoader = launcherLoader;
	        if (loadDependencies) {
	            initClasspath(readAdditionalFolder(), launcherLoader);
	            initClasspath(readMavenDependencies(), launcherLoader);
	            // pluginClassLoader = new EClassLoader(pluginJars, appLoader);
	        }
	        if (enableAssertions) {
	            launcherLoader.setDefaultAssertionStatus(true);
	            pluginClassLoader.setDefaultAssertionStatus(true);
	        }
	    }
	    
	    private static void initClasspath(URL[] urls, ClassLoader launcherLoader) {
	        for (URL url : urls) {
	            Object result = callByReflection(launcherLoader, "java.net.URLClassLoader", "addURL",
	                    new Class[]{URL.class}, launcherLoader, new Object[]{url});
	        }
	    }

	    private static URL[] readAdditionalFolder() {
	        List<URL> urls = new ArrayList<URL>();

	        File additionalFolder = new File("additional");
	        if (additionalFolder.exists() && additionalFolder.isDirectory()) {
	            File[] files = additionalFolder.listFiles(new FileFilter() {
	                public boolean accept(File pathname) {
	                    return pathname.getName().endsWith(".jar");
	                }
	            });

	            for (File file : files) {
	                try {
	                    urls.add(file.toURI().toURL());
	                    logger.info("Add " + file.getAbsoluteFile() + " to classpath...");
	                } catch (MalformedURLException e) {
	                    logger.log(Level.SEVERE, "Add " + file.getAbsoluteFile() + " to classpath...", e);
	                }
	            }
	        }

	        return urls.toArray(new URL[urls.size()]);
	    }

	    private static URL[] readMavenDependencies() {
	        String mavenDependenciesResourceName = "maven.dependencies";
	        URL mavenDependencies = ClassLoader.getSystemResource(mavenDependenciesResourceName);
	        if (mavenDependencies == null) {
	            return new URL[0];
	        }

	        List<URL> urls = new ArrayList<URL>();
	        List<File> repositories = new ArrayList<File>();
	        String mavenRepository = ".m2" + File.separator + "repository";

	        String homeDirProp = System.getProperty("user.home");
	        if (homeDirProp != null) {
	            File homeDir = new File(homeDirProp);
	            File file = new File(homeDir, mavenRepository);
	            if (file.canRead()) {
	                repositories.add(file);
	                logger.log(Level.FINE, "Found repository {0}", file);
	            } else if (file.exists()) {
	                logger.log(Level.FINE, "Can't read repository {0}", file);
	            } else {
	                logger.log(Level.FINE, "Not found repository {0}", file);
	            }
	        }

	        if (mavenDependencies.getProtocol().equals("jar")) {
	            String path = mavenDependencies.getPath();
	            if (path.startsWith("file:") && path.endsWith("!/" + mavenDependenciesResourceName)) {
	                String jarPath = path.substring("file:".length(), path.length() - "!/".length()
	                        - mavenDependenciesResourceName.length());
	                File jarDir = new File(jarPath).getParentFile();
	                File file = new File(jarDir, mavenRepository);
	                if (file.canRead()) {
	                    repositories.add(file);
	                    logger.log(Level.FINE, "Found repository {0}", file);
	                } else if (file.exists()) {
	                    logger.log(Level.FINE, "Can't read repository {0}", file);
	                } else {
	                    logger.log(Level.FINE, "Not found repository {0}", file);
	                }
	            }
	        }
	        try {
	            Properties properties = PropertiesUtils.load(mavenDependenciesResourceName);
	            for (Object dependency : properties.keySet()) {
	                for (File repository : repositories) {
	                    File file = new File(repository, dependency.toString());
	                    if (file.canRead()) {
	                        URL url = file.toURI().toURL();
	                        urls.add(url);
	                        logger.log(Level.FINE, "Add {0} to class path", url);
	                        break;
	                    } else if (file.exists()) {
	                        logger.log(Level.FINE, "Can't read {0}", file);
	                    } else {
	                        logger.log(Level.FINEST, "Not found {0}", file);
	                    }
	                }
	            }
	        } catch (Exception e) {
	            logger.log(Level.SEVERE, "Error reading maven dependencies", e);
	        }

	        return urls.toArray(new URL[urls.size()]);
	    }

	    private static Object callByReflection(ClassLoader classLoader, String className, String methodName,
	            Class[] argTypes, Object obj, Object[] argValues) {
	        try {
	            Class mainClass = classLoader.loadClass(className);
	            Method method = mainClass.getDeclaredMethod(methodName, argTypes);
	            method.setAccessible(true);
	            return method.invoke(obj, argValues);
	        } catch (ClassNotFoundException e) {
	            logger.log(Level.SEVERE, "Can't invoke Electric", e);
	        } catch (NoSuchMethodException e) {
	            logger.log(Level.SEVERE, "Can't invoke Electric", e);
	        } catch (IllegalAccessException e) {
	            logger.log(Level.SEVERE, "Can't invoke Electric", e);
	        } catch (InvocationTargetException e) {
	            logger.log(Level.SEVERE, "Error in Electric invocation", e.getTargetException());
	        }
	        return null;
	    }

	    private static class EClassLoader extends URLClassLoader {
	        private EClassLoader(URL[] urls, ClassLoader parent) {
	            super(urls, parent);
	        }

	        @Override
	        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
	            if (name.startsWith("com.sun.electric.plugins.") || name.startsWith("com.sun.electric.scala.")) {
	                Class c = findLoadedClass(name);
	                if (c == null) {
	                    String path = name.replace('.', '/').concat(".class");
	                    URL url = getResource(path);
	                    if (url != null) {
	                        try {
	                            DataInputStream in = new DataInputStream(url.openStream());
	                            int len = in.available();
	                            byte[] ba = new byte[len];
	                            in.readFully(ba);
	                            in.close();
	                            c = defineClass(name, ba, 0, len);
	                        } catch (IOException e) {
	                            throw new ClassNotFoundException(name);
	                        }
	                    } else {
	                        throw new ClassNotFoundException(name);
	                    }
	                }
	                if (resolve) {
	                    resolveClass(c);
	                }
	                return c;
	            }
	            return super.loadClass(name, resolve);
	        }
	    }
/** Get the Menu Bar. Unfortunately named because getMenuBar() already exists */
    public static EMenuBar getEMenuBar() { return menuBar.getMenuBarGroup(); }

    public static synchronized List<EMenuBar.Instance> getMenuBars() {
        ArrayList<EMenuBar.Instance> menuBars = new ArrayList<EMenuBar.Instance>();
        for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
//        	WindowFrame wf = it.next();
//        	menuBars.add(wf.getFrame().getTheMenuBar());
        }
        return menuBars;
    }
    
	public static Icon getFrameIcon()
	{
		return null;
	}

	public static void InitializeWindows(Main thing) {
		try{
            menuBar = MenuCommands.menuBar().genInstance();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        thing.setJMenuBar(menuBar);

		// create the tool bar
		Main.toolBar = new ToolBar();
		thing.getContentPane().add(toolBar, BorderLayout.NORTH);
		// create the status bar
		thing.sb = new StatusBar(null, true);
		thing.getContentPane().add(thing.sb, BorderLayout.SOUTH);
		
		WindowFrame.createEditWindow(null);
		toplevel = thing;
	}
	
	public static Dimension getScreenSize()
	{
		return new Dimension(scrnSize);
	}

	public static MessagesWindow getMessagesWindow() { return messagesWindow; }
	
	public static Point getCursorLocation() {
		return toplevel.getLocationOnScreen();
	}

	public static Component getCurrentJFrame() {
		// TODO Auto-generated method stub
		return null;
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
    
	public static Cursor getCurrentCursor() { return cursor; }

	public static synchronized void setCurrentCursor(Cursor newcursor)
	{
        cursor = newcursor;
        setCurrentCursorPrivate(cursor);
    }

    private static synchronized void setCurrentCursorPrivate(Cursor cursor)
    {
        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
        {
            WindowFrame wf = it.next();
            wf.setCursor(cursor);
        }
	}
    
	/**
	 * Method to add an internal frame to the desktop.
	 * This only makes sense in MDI mode, where the desktop has multiple subframes.
	 * @param jif the internal frame to add.
	 */
	public static void addToDesktop(JInternalFrame jif)
	{
        if (desktop.isVisible() && !Job.isClientThread())
            SwingUtilities.invokeLater(new ModifyToDesktopSafe(jif, true)); else
            	(new ModifyToDesktopSafe(jif, true)).run();
    }
	
	/**
	 * Method to remove an internal frame from the desktop.
	 * This only makes sense in MDI mode, where the desktop has multiple subframes.
	 * @param jif the internal frame to remove.
	 */
	public static void removeFromDesktop(JInternalFrame jif)
	{
        if (desktop.isVisible() && !Job.isClientThread())
            SwingUtilities.invokeLater(new ModifyToDesktopSafe(jif, false)); else
            	(new ModifyToDesktopSafe(jif, false)).run();
    }

    private static class ModifyToDesktopSafe implements Runnable
    {
        private JInternalFrame jif;
        private boolean add;

        private ModifyToDesktopSafe(JInternalFrame jif, boolean add) { this.jif = jif;  this.add = add; }

        public void run()
        {
        	if (add)
        	{
	            desktop.add(jif);
	            try
	            {
	            	jif.show();
	            } catch (ClassCastException e)
	            {
	            	// Jake Baker keeps getting a ClassCastException here, so for now, let's catch it
	            	System.out.println("ERROR: Could not show new window: " + e.getMessage());
	            }
        	} else
        	{
                desktop.remove(jif);
        	}
        }
    }
    
    /** Get the Menu Bar. Unfortunately named because getMenuBar() already exists */
    public static EMenuBar.Instance getTheMenuBar() { return menuBar; }

    /**
     * Get the tool bar associated with this TopLevel
     * @return the ToolBar.
     */
    public static ToolBar getToolBar() { return toolBar; }
    
    public static synchronized List<ToolBar> getToolBars() {
        ArrayList<ToolBar> toolBars = new ArrayList<ToolBar>();
        for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
        	WindowFrame wf = it.next();
//        	toolBars.add(wf.getFrame().getToolBar());
        }
        return toolBars;
    }
    
    /**
	 * Method to return status bar associated with this TopLevel.
	 * @return the status bar associated with this TopLevel.
	 */
	public static StatusBar getStatusBar() { return sb; }
	
    /**
     * Method to return the speed of double-clicks (in milliseconds).
     * @return the speed of double-clicks (in milliseconds).
     */
    public static int getDoubleClickSpeed() { return doubleClickDelay; }
    
    /**
     * Method to return a list of possible window areas.
     * On MDI systems, there is just one window areas.
     * On SDI systems, there is one window areas for each display head on the computer.
     * @return an array of window areas.
     */
    public static Rectangle [] getWindowAreas()
	{
		Rectangle [] areas;
		areas = getDisplays();
		return areas;
	}
    
    /**
     * Method to return a list of display areas, one for each display head on the computer.
     * @return an array of display areas.
     */
    public static Rectangle [] getDisplays()
	{
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice [] gs = ge.getScreenDevices();
		Rectangle [] areas = new Rectangle[gs.length];
		for (int j = 0; j < gs.length; j++)
		{
			GraphicsDevice gd = gs[j];
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			areas[j] = gc.getBounds();
		}
		return areas;
	}
    
    public static void addModelessDialog(Window dialog) { modelessDialogs.add(dialog); }
    
    public static void removeModelessDialog(JFrame dialog) { modelessDialogs.remove(dialog); }
}
