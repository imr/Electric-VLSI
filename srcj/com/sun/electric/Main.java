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
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.MenuCommands;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Method;

/**
 * This class initializes the User Interface.
 * It is the main class of Electric.
 */
public final class Main
{
	public static void main(String[] args)
	{
		// initialize Mac OS 10 if applicable
		macOSXRegistration();

		// convert args to array list
        ArrayList argsList = new ArrayList();
        for (int i=0; i<args.length; i++) argsList.add(args[i]);

		TopLevel.OSInitialize();

		// initialize database
		new InitDatabase();

        if (hasCommandLineOption(argsList, "-m")) {
            // set multiheaded option here
        }
        String beanShellScript = getCommandLineOption(argsList, "-s");
        if (!openCommandLineLibs(argsList)) {
            // open default library (or maybe open none at all?)
            Library mainLib = Library.newInstance("noname", null);
            Library.setCurrent(mainLib);
			WindowFrame window1 = WindowFrame.createEditWindow(null);
        }

        // run script
        if (beanShellScript != null) EvalJavaBsh.tool.runScript(beanShellScript);
	}

    /** check if command line option 'option' present in 
     * command line args. If present, return true and remove if from the list.
     * Otherwise, return false.
     */
    private static boolean hasCommandLineOption(ArrayList argsList, String option) 
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
    private static String getCommandLineOption(ArrayList argsList, String option)
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
    private static boolean openCommandLineLibs(ArrayList argsList)
    {
        boolean openedALib = false;
        for (int i=0; i<argsList.size(); i++) {
            String arg = (String)argsList.get(i);
            if (arg.startsWith("-")) {
                System.out.println("Command line option "+arg+" not understood, ignoring.");
                continue;
            }
            System.out.println("Opening library "+arg);
    		MenuCommands.ReadBinaryLibrary job = new MenuCommands.ReadBinaryLibrary(arg);
            openedALib = true;
        }
        return openedALib;
    }

	/**
	 * Class to init all technologies.
	 */
	protected static class InitDatabase extends Job
	{
		protected InitDatabase()
		{
			super("Init database", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			// initialize all of the technologies
			Technology.initAllTechnologies();

			// initialize all of the tools
			Tool.initAllTools();

			// initialize the constraint system
			Layout con = Layout.getConstraint();
			Constraints.setCurrent(con);
		}
	}
	
	/**
	 * Generic registration with the Mac OS X application menu.
	 * Attempts to register with the Apple EAWT.
	 * This method calls OSXAdapter.registerMacOSXApplication().
	 */
	private static void macOSXRegistration()
	{
		try
		{
			Class osxAdapter = Class.forName("com.sun.electric.tool.user.ui.OSXAdapter");
			Class[] defArgs = {};
			Method registerMethod = osxAdapter.getDeclaredMethod("registerMacOSXApplication", defArgs);
			if (registerMethod != null)
			{
				Object[] args = {};
				registerMethod.invoke(osxAdapter, args);
			}
		} catch (NoClassDefFoundError e)
		{
			// This will be thrown if the OSXAdapter is loaded on a system without the EAWT
			// because OSXAdapter extends ApplicationAdapter in its def
			System.err.println("NoClassDefFoundError: This version of Mac OS X does not support the Apple EAWT.  Application Menu handling has been disabled (" + e + ")");
		} catch (ClassNotFoundException e)
		{
			// If the class is not found, then perhaps this isn't a Macintosh, and we should stop
		} catch (Exception e)
		{
			System.err.println("Exception while loading the OSXAdapter:");
			e.printStackTrace();
		}
	}

}
