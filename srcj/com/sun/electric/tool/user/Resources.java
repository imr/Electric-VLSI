/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WaveformWindow.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import com.sun.electric.Main;
import javax.swing.ImageIcon;
import java.net.URL;

/**
 * public class to handle resources like icons/images.
 */
public class Resources {
	private static final String resourceLocation = "resources/";

	// Location of valid 3D plugin
	public static final String plugin3D = "com.sun.electric.plugins.j3d.View3DWindow";

	/**
	 * Method to load a valid icon stored in resources package under the given class
	 * @param theClass class path where the icon resource is stored under
	 * @param iconName icon name
	 * @return
	 */
	public static ImageIcon getResource(Class theClass, String iconName)
	{
		return (new ImageIcon(getURLResource(theClass, iconName)));
	}

	/**
	 * Method to get URL path for a resource stored in resources package
	 * under the given class
	 * @param theClass class path where resource is stored under
	 * @param resourceName resource name
	 * @return
	 */
	public static URL getURLResource(Class theClass, String resourceName)
	{
		return (theClass.getResource(resourceLocation+resourceName));
	}

	/**
	 * Method to obtain main 3D class
	 * @return
	 */
	public static Class get3DMainClass()
	{
		Class view3DClass = null;
		try
        {
            view3DClass = Class.forName(plugin3D);

        } catch (ClassNotFoundException e)
        {
            if (Main.getDebug()) System.out.println("Can't find 3D View plugin: " + e.getMessage());
        } catch (Error e)
        {
            if (Main.getDebug()) System.out.println("Java3D not installed: " + e.getMessage());
        }
		return (view3DClass);
	}
}
