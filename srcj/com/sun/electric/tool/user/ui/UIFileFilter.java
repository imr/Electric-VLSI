/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UIFileFilter.java
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
package com.sun.electric.tool.user.ui;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.filechooser.FileFilter;

/**
 * User interface class to filter file types
 *
 * @author  Steven Rubin
 */
public class UIFileFilter extends FileFilter
{
	String desc;
	List extensions;

	/** Creates a new instance of UIFileFilter */
	public UIFileFilter()
	{
		extensions = new ArrayList();
	}

	public boolean accept(java.io.File f)
	{
		if (f == null) return false;
		if (f.isDirectory()) return true;
		String filename = f.getName();
		int i = filename.lastIndexOf('.');
		if (i < 0) return false;
		String thisExtension = filename.substring(i+1);
		if (thisExtension == null) return false;
		for(Iterator it = extensions.iterator(); it.hasNext(); )
		{
			String extension = (String)it.next();
			if (extension.equalsIgnoreCase(thisExtension)) return true;
		}
		return false;
	}

	public void addExtension(String extension)
	{
		extensions.add(extension);
	}

	public String getDescription()
	{
		return desc;
	}

	public void setDescription(String desc)
	{
		this.desc = desc;
	}

}
