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
		// initialize Mac OS 10 if applicable
		Main.MacOSXInterface.registerMacOSXApplication();

        // ignore launcher if specified to do so
        for (int i=0; i<args.length; i++) {
            String str = args[i];
            if (str.equals("-NOMINMEM") || str.equals("-help") || str.equals("-version")) {
                // just start electric
                Main.main(args);
                return;
            }
        }

		// launching is different on different computers
		try{
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("linux") || osName.startsWith("solaris") ||
				osName.startsWith("sunos") || osName.startsWith("mac"))
			{
				// able to "exec" a new JVM: do it with the proper memory limit
				invokeElectric(args, "java");
			} else if (osName.startsWith("windows"))
			{
				invokeElectric(args, "javaw");
			} else
			{
				// not able to relaunch a JVM: just start Electric
				Main.main(args);
			}
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
		int maxMemWanted = User.getMemorySize();
		if (maxMemWanted <= maxMem)
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
		command += " -mx" + maxMemWanted + "m com.sun.electric.Main";
        System.out.println("Rerunning Electric with memory footprint of "+maxMemWanted+"m because "+maxMem+"m is too small");
        for (int i=0; i<args.length; i++) command += " " + args[i];
        //System.out.println("exec: "+command);
		try
		{
			runtime.exec(command);
            // Sometimes Mac process doesn't finish nicely for some reason, try and kill it here
            System.exit(0);
		} catch (java.io.IOException e)
		{
			Main.main(args);
		}
	}
}
