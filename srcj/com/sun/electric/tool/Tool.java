/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Tool.java
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
package com.sun.electric.tool;

import com.sun.electric.database.variable.ElectricObject;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class represents a Tool in Electric.  It's here mostly for the name
 * of the tool and the variables attached.  The User holds
 * variables that keep track of the currently selected object, and other
 * useful information.
 */
public class Tool extends ElectricObject
{
	// The name of this tool
	private String toolName;
	private int toolState;
	private int toolIndex;

	// the static list of all tools
	private static List tools = new ArrayList();
	private static int toolNumber = 0;

	/** set if tool is on */								private static final int TOOLON =             01;
	/** set if tool is running in background */				private static final int TOOLBG =             02;
	/** set if tool will fix errors */						private static final int TOOLFIX =            04;
	/** set if tool is coded in interpretive language */	private static final int TOOLLANG =          010;
	/** set if tool functions incrementally */				private static final int TOOLINCREMENTAL =   020;
	/** set if tool does analysis */						private static final int TOOLANALYSIS =      040;
	/** set if tool does synthesis */						private static final int TOOLSYNTHESIS =    0100;

	private Tool()
	{
	}

	/**
	 * Routine to create a new Tool with a given name.
	 * @param toolName the name of the Tool.
	 * @return the newly created Tool.
	 */
	public static final Tool newInstance(String toolName)
	{
		Tool t = new Tool();
		t.toolName = toolName;
		t.toolIndex = toolNumber++;
		return t;
	}

	/**
	 * Routine to send a message to a Tool.
	 * @param tool the tool to send the message to.
	 * @param cmd the command to send to the Tool.
	 * @param msg the parameter to send to the Tool.
	 */
	public static void askTool(Tool tool, String cmd, String msg)
	{
	}

	/**
	 * Routine to find the Tool with a specified name.
	 * @param name the name of the desired Tool.
	 * @return the Tool with that name, or null if no tool matches.
	 */
	public static Tool findTool(String name)
	{
		for (int i = 0; i < tools.size(); i++)
		{
			Tool t = (Tool) tools.get(i);
			if (t.getName().equals(name))
				return t;
		}
		return null;
	}

	/**
	 * Routine to return an Iterator over all of the Tools in Electric.
	 * @return an Iterator over all of the Tools in Electric.
	 */
	public static Iterator getTools()
	{
		return tools.iterator();
	}

	/**
	 * Routine to return the number of Tools.
	 * @return the number of Tools.
	 */
	public static int getNumTools()
	{
		return tools.size();
	}

	/**
	 * Routine to return the name of this Tool.
	 * @return the name of this Tool.
	 */
	public String getName() { return toolName; }

	/**
	 * Routine to return the index of this Tool.
	 * Each tool has a 0-based index that can be used to access arrays of Tools.
	 * @return the index of this Tool.
	 */
	public int getIndex() { return toolIndex; }

	/**
	 * Routine to determine whether a variable name on Tool is deprecated.
	 * Deprecated variable names are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param name the name of the variable.
	 * @return true if the variable name is deprecated.
	 */
	public boolean isDeprecatedVariable(String name)
	{
		if (name.equals("NET_auto_name") ||
			name.equals("NET_use_port_names") ||
			name.equals("NET_compare_hierarchy") ||
			name.equals("D") ||
			name.equals("<") ||
			name.equals("USER_alignment_obj") ||
			name.equals("USER_alignment_edge") ||
			name.equals("s") ||
			name.equals("DRC_pointout")) return true;
		return false;
	}

	/**
	 * Returns a printable version of this Tool.
	 * @return a printable version of this Tool.
	 */
	public String toString()
	{
		return "Tool '" + toolName;
	}
}
