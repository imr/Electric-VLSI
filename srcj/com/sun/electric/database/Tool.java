package com.sun.electric.database;

//import java.util.*;

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
	private String name;

	/** set if tool is on */								public static final int TOOLON=                    01;
	/** set if tool is running in background */				public static final int TOOLBG=                    02;
	/** set if tool will fix errors */						public static final int TOOLFIX=                   04;
	/** set if tool is coded in interpretive language */	public static final int TOOLLANG=                 010;
	/** set if tool functions incrementally */				public static final int TOOLINCREMENTAL=          020;
	/** set if tool does analysis */						public static final int TOOLANALYSIS=             040;
	/** set if tool does synthesis */						public static final int TOOLSYNTHESIS=           0100;

	protected Tool()
	{
//		super(cptr);
	}

	/** Get the name of this tool */
	public String getName()
	{
		return name;
	}

	/** Initialize this tool with a name */
	protected void init(String name)
	{
		this.name = name;
	}

	public String toString()
	{
		return "Tool '" + name;
	}
}
