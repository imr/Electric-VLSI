/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Launcher.java
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

import com.sun.electric.tool.Regression;

import java.io.File;

/**
 * This class initializes the Electric by re-launching a JVM with sufficient memory.
 * It is the main entrypoint of the system.
 */
public final class Launcher
{
	private static final boolean enableAssertions = true;

	private Launcher() {}

	/**
	 * The main entry point of Electric.
	 * The "main" method in this class checks to make sure that
	 * the JVM has enough memory.  If not, it invokes a new JVM
	 * with the proper amount.  If so, it goes directly to Main.main
	 * to start Electric.
	 * @param args the arguments to the program.
	 */
	public static void main(String[] args)
	{
        // ignore launcher if specified to do so
        for (int i=0; i<args.length; i++) {
            String str = args[i];
            if (str.equals("-NOMINMEM") || str.equals("-help") ||
                str.equals("-version") || str.equals("-v")) {
                // just start electric
                Main.main(args);
                return;
            }
        }

        String program = "java";
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) program = javaHome + File.separator + "bin" + File.separator + program;

        if (args.length >= 2 && args[0].equals("-regression") ||
                args.length >= 3 && args[0].equals("-debug") && args[1].equals("-regression") ||
                args.length >= 4 && args[0].equals("-threads") && args[2].equals("-regression") ||
                args.length >= 4 && args[0].equals("-logging") && args[2].equals("-regression")) {
            System.exit(invokeRegression(args) ? 0 : 1);
            return;
        }

		// launching is different on different computers
		try{
            if (program != null)
                invokeElectric(args, program);
            else
				// not able to relaunch a JVM: just start Electric
				Main.main(args);
		} catch (Exception e)
		{
			// problem figuring out what computer this is: just start Electric
			e.printStackTrace(System.out);
			Main.main(args);
		}
	}

	private static final String[] propertiesToCopy =
	{
		"user.home"
	};

	private static void invokeElectric(String[] args, String program)
	{
		// see if the required amount of memory is already present
		Runtime runtime = Runtime.getRuntime();
		long maxMem = runtime.maxMemory() / 1000000;
        long maxPermWanted = StartupPrefs.getPermSpace();
		int maxMemWanted = StartupPrefs.getMemorySize();
		if (maxMemWanted <= maxMem && maxPermWanted == 0)
		{
			// already have the desired amount of memory: just start Electric
			if (enableAssertions)
				ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
			Main.main(args);
			return;
		}

//        // get location of jar file
//        URL electric = Launcher.class.getResource("Main.class");
//        if (electric.getProtocol().equals("jar")) {
//            String file = electric.getFile();
//            file = file.replaceAll("file:", "");
//            file = file.replaceAll("!.*", "");
//        }

        // build the command line for reinvoking Electric
		String command = program;
		command += " -cp " + Main.getJarLocation();
        command += " -ss2m";
		if (enableAssertions)
			command += " -ea"; // enable assertions
		command += " -mx" + maxMemWanted + "m";
        if (maxPermWanted > 0)
            command += " -XX:MaxPermSize=" + maxPermWanted + "m";
        for(int i=0; i<propertiesToCopy.length; i++)
        {
	        String propValue = System.getProperty(propertiesToCopy[i]);
	        if (propValue != null && propValue.length() > 0)
	        {
	        	if (propValue.indexOf(' ') >= 0) propValue = "\"" + propValue + "\"";
	        	command += " -D" + propertiesToCopy[i] + "=" + propValue;
	        }
        }
        command += " com.sun.electric.Main";
        for (int i=0; i<args.length; i++) command += " " + args[i];

        if (maxMemWanted > maxMem)
            System.out.println("Current Java memory limit of " + maxMem +
            	"MEG is too small, rerunning Electric with a memory limit of " + maxMemWanted + "MEG");
        if (maxPermWanted > 0)
            System.out.println("Setting maximum permanent space (2nd GC) to " + maxPermWanted + "MEG");
		try
		{
			runtime.exec(command);
            // Sometimes process doesn't finish nicely for some reason, try and kill it here
            System.exit(0);
		} catch (java.io.IOException e)
		{
			Main.main(args);
		}
	}

    private static boolean invokeRegression(String[] args) {
        String javaOptions = "";
        String electricOptions = " -debug";
        int regressionPos = 0;
        if (args[0].equals("-threads")) {
            regressionPos = 2;
            electricOptions += " -threads " + args[1];
        } else if (args[0].equals("-logging")) {
            regressionPos = 2;
            electricOptions += " -logging " + args[1];
        }
        else if (args[0].equals("-debug"))
        {
            regressionPos = 1;
            // no need of adding the -debug flag
        }
        String script = args[regressionPos + 1];

        for (int i = regressionPos + 2; i < args.length; i++)
            javaOptions += " " + args[i];
        Process process = null;
        try {
            process = Main.invokePipeserver(javaOptions, electricOptions);
            return Regression.runScript(process, script);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
