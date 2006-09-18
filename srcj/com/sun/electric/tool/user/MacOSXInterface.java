/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MacOSXInterface.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user;

/*
 * HAVING TROUBLE COMPILING THIS MODULE?
 *
 * If the following import statements are failing to compile,
 * you are probably building Electric on a non-Macintosh system.
 * To solve the errors, simply delete this entire module from the build.
 *
 * See http://www.staticfreesoft.com/jmanual/mchap01-03.html for details.
 */
import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;
import com.sun.electric.Main;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.dialogs.PreferencesFrame;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.menus.HelpMenu;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Class for initializing the Macintosh OS X world.
 */
class MacOSXInterface extends ApplicationAdapter
{
	private static MacOSXInterface adapter = null;
	private static Application application = null;
    private static List<String> argsList; // references to args list to add the file that triggers the opening
    protected Job initJob;

	/**
	 * Method to initialize the Mac OS/X interface.
	 * @param list the arguments to the application.
	 */
	private MacOSXInterface(List<String> list)
    {
        argsList = list;
    }

	/**
	 * Class for informing the Mac OS/X interface of the initialization job.
	 * This is done to handle initial library requests at the proper time.
	 * @param ij the initializtion job.
	 */
	public static void setInitJob(Job ij) { adapter.initJob = ij; }

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
	 * Method to handle open-file requests.
	 */
    public void handleOpenFile(ApplicationEvent ae)
    {
        ae.setHandled(true);
        String filename = ae.getFilename();

        // First open
        if (!UserInterfaceMain.initializationFinished)
        {
            argsList.add(filename);
        }
        else
        {
            // Handle the rest of double-clicks on files.
            List<String> list = new ArrayList<String>(1);
            list.add(filename);
            Main.openCommandLineLibs(list);
        }
        URL dirUrl = TextUtils.makeURLToFile(filename);
        String dirString = TextUtils.getFilePath(dirUrl);
        User.setWorkingDirectory(dirString);
    }

	/**
	 * Method to initialize the Macintosh OS X environment.
	 * @param argsList the arguments to the application.
	 */
	public static void registerMacOSXApplication(List<String> argsList)
	{
		// tell it to use the system menubar
		System.setProperty("com.apple.macos.useScreenMenuBar", "true");
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		// set the name of the leftmost pulldown menu (under the apple) to "Electric"
		System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Electric");

		// create Mac objects for handling the "Electric" menu
		if (application == null) application = new Application();
		if (adapter == null) adapter = new MacOSXInterface(argsList);
		application.addApplicationListener(adapter);
		application.setEnabledPreferencesMenu(true);
	}
}
