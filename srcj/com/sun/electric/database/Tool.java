package com.sun.electric.database;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class represents a Tool in Electric.  It's here mostly for the name
 * of the tool and the variables attached.  The UI tool ("User") holds
 * variables that keep track of the currently selected object, and other
 * useful information.  Use <code>Electric.askTool</code> to cause tools
 * to do things.
 */
public class Tool extends ElectricObject
{
	// The name of this tool
	private String toolName;
	private int toolState;

	// the static list of all tools
	private static List tools = new ArrayList();

	/** set if tool is on */								public static final int TOOLON=             01;
	/** set if tool is running in background */				public static final int TOOLBG=             02;
	/** set if tool will fix errors */						public static final int TOOLFIX=            04;
	/** set if tool is coded in interpretive language */	public static final int TOOLLANG=          010;
	/** set if tool functions incrementally */				public static final int TOOLINCREMENTAL=   020;
	/** set if tool does analysis */						public static final int TOOLANALYSIS=      040;
	/** set if tool does synthesis */						public static final int TOOLSYNTHESIS=    0100;

	private Tool(String toolName)
	{
		this.toolName = toolName;
	}

	/** Initialize this tool with a name */
	public static final Tool newInstance(String toolName)
	{
		Tool t = new Tool(toolName);
		return t;
	}

	public static void askTool(Tool t, String cmd, String msg)
	{
	}

	/** Find the tool with a particular name.  See the Electric internals
	 * manual for examples of Tools.
	 * @param name the name of the desired tool
	 * @return the Tool with the same name, or null if no tool matches. */
	public static Tool findTool(String name)
	{
		for (int i = 0; i < tools.size(); i++)
		{
			Tool t = (Tool) tools.get(i);
			if (t.getName().equals(name))
				return t;
		}
		System.out.println("Couldn't find tool named '" + name + "'.");
		return null;
	}

	/** Get an iterator over all tools. */
	public static Iterator getTools()
	{
		return tools.iterator();
	}

	/** Get the name of this tool */
	public String getName() { return toolName; }

	public String toString()
	{
		return "Tool '" + toolName;
	}
}
