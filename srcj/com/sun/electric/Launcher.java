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

import com.sun.electric.tool.Regression;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.user.User;

import java.net.URL;

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
//        if (Client.isOSWindows()) program = "javaw";

        if (args.length >= 1 && args[0].equals("-regression")) {
            invokeRegression(args, program);
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

	private static void invokeElectric(String[] args, String program)
	{
		// see if the required amount of memory is already present
		Runtime runtime = Runtime.getRuntime();
		long maxMem = runtime.maxMemory() / 1000000;
        long maxPermWanted = User.getPermSpace();
		int maxMemWanted = User.getMemorySize();
		if (maxMemWanted <= maxMem && maxPermWanted == 0)
		{
			// already have the desired amount of memory: just start Electric
			if (enableAssertions)
				ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);
			Main.main(args);
			return;
		}

        // get location of jar file
        String jarfile = "electric.jar";
        URL electric = Launcher.class.getResource("Main.class");
        if (electric.getProtocol().equals("jar")) {
            String file = electric.getFile();
            file = file.replaceAll("file:", "");
            file = file.replaceAll("!.*", "");
            jarfile = file;
        }

		String command = program;
		command += " -cp " + System.getProperty("java.class.path",".");
//		command += " -mx" + maxMemWanted + "m -jar " + jarfile;
        command += " -ss2m";
		if (enableAssertions)
			command += " -ea"; // enable assertions
		command += " -mx" + maxMemWanted + "m ";
        if (maxPermWanted > 0)
            command += " -XX:MaxPermSize=" + maxPermWanted + "m ";
        command += "com.sun.electric.Main";
        if (maxMemWanted > maxMem)
            System.out.println("Current Java memory limit of "+maxMem+"MEG is too small, rerunning Electric with a memory limit of "+maxMemWanted+"MEG");
        if (maxPermWanted > 0)
            System.out.println("Setting maximum permanent space (2nd GC) to " + maxPermWanted + "MEG");
        for (int i=0; i<args.length; i++) command += " " + args[i];
        //System.out.println("exec: "+command);
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
    
    private static void invokeRegression(String[] args, String program) {
        String jarfile = "electric.jar";
        URL electric = Launcher.class.getResource("Main.class");
       if (electric.getProtocol().equals("jar")) {
            String file = electric.getFile();
            file = file.replaceAll("file:", "");
            file = file.replaceAll("!.*", "");
            jarfile = file;
        }
        
        String script = args[1];
        String command = program;
        command += " -ea";
        for (int i = 2; i < args.length; i++)
            command += " " + args[i];
        command += " -jar " + jarfile + " -batch";
        System.out.println("exec: " + command);
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(command);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        Regression.runScript(script);
    }
}
