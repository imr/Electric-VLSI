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
import com.sun.electric.database.change.Change;
import com.sun.electric.tool.user.User;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class represents a Tool in Electric.  It's here mostly for the name
 * of the tool and the variables attached.  The User holds
 * variables that keep track of the currently selected object, and other
 * useful information.
 */
public class Tool extends ElectricObject implements Change
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
//	/** set if tool is coded in interpretive language */	private static final int TOOLLANG =          010;
	/** set if tool functions incrementally */				private static final int TOOLINCREMENTAL =   020;
	/** set if tool does analysis */						private static final int TOOLANALYSIS =      040;
	/** set if tool does synthesis */						private static final int TOOLSYNTHESIS =    0100;

	/**
	 * The constructor for Tool is only called by subclasses.
	 * @param toolName the name of this tool.
	 */
	protected Tool(String toolName)
	{
		this.toolName = toolName;
		this.toolState = 0;
		this.toolIndex = toolNumber++;
		tools.add(this);
	}

	/**
	 * This is called once, at the start of Electric, to initialize the Tools.
	 * Because of Java's "lazy evaluation", the only way to force the Tool constructors to fire
	 * and build a proper list of Tools, each class must somehow be referenced.
	 * So, each Tool is listed here.  If a new Tool is created, this must be updated.
	 */
	public static void initAllTools()
	{
		// Because of lazy evaluation, tools aren't initialized unless they're referenced here
		User.tool.init();
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
	 * Routine to set this Tool to be on.
	 * Tools that are "on" are running incrementally, and get slices and broadcasts.
	 */
	public void setOn() { toolState |= TOOLON; }

	/**
	 * Routine to set this Tool to be off.
	 * Tools that are "on" are running incrementally, and get slices and broadcasts.
	 */
	public void clearOn() { toolState &= ~TOOLON; }

	/**
	 * Routine to tell whether this Tool is on.
	 * Tools that are "on" are running incrementally, and get slices and broadcasts.
	 * @return true if this Tool is on.
	 */
	public boolean isOn() { return (toolState & TOOLON) != 0; }

	/**
	 * Routine to set this Tool to be in the background.
	 */
	public void setBackground() { toolState |= TOOLBG; }

	/**
	 * Routine to set this Tool to be in the foreground.
	 */
	public void clearBackground() { toolState &= ~TOOLBG; }

	/**
	 * Routine to tell whether this Tool is in the background.
	 * @return true if this Tool is in the background.
	 */
	public boolean isBackground() { return (toolState & TOOLBG) != 0; }

	/**
	 * Routine to set this Tool to fix errors.
	 */
	public void setFixErrors() { toolState |= TOOLFIX; }

	/**
	 * Routine to set this Tool to fix errors.
	 */
	public void clearFixErrors() { toolState &= ~TOOLFIX; }

	/**
	 * Routine to tell whether this Tool fixes errors.
	 * @return true if this Tool fixes errors.
	 */
	public boolean isFixErrors() { return (toolState & TOOLFIX) != 0; }

	/**
	 * Routine to set this Tool to be incremental.
	 */
	public void setIncremental() { toolState |= TOOLINCREMENTAL; }

	/**
	 * Routine to set this Tool to be incremental.
	 */
	public void clearIncremental() { toolState &= ~TOOLINCREMENTAL; }

	/**
	 * Routine to tell whether this Tool is incremental.
	 * @return true if this Tool is incremental.
	 */
	public boolean isIncremental() { return (toolState & TOOLINCREMENTAL) != 0; }

	/**
	 * Routine to set this Tool to be analysis.
	 */
	public void setAnalysis() { toolState |= TOOLANALYSIS; }

	/**
	 * Routine to set this Tool to be analysis.
	 */
	public void clearAnalysis() { toolState &= ~TOOLANALYSIS; }

	/**
	 * Routine to tell whether this Tool does analysis.
	 * @return true if this Tool does analysis.
	 */
	public boolean isAnalysis() { return (toolState & TOOLANALYSIS) != 0; }

	/**
	 * Routine to set this Tool to be synthesis.
	 */
	public void setSynthesis() { toolState |= TOOLSYNTHESIS; }

	/**
	 * Routine to set this Tool to be synthesis.
	 */
	public void clearSynthesis() { toolState &= ~TOOLSYNTHESIS; }

	/**
	 * Routine to tell whether this Tool does synthesis.
	 * @return true if this Tool does synthesis.
	 */
	public boolean isSynthesis() { return (toolState & TOOLSYNTHESIS) != 0; }

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

	public void init() {}
	public void request(String cmd) {}
	public void examineCell(com.sun.electric.database.hierarchy.Cell cell) {}
	public void slice() {}

	public void startBatch(Tool tool, boolean undoRedo) {}
	public void endBatch() {}

	public void startChange(ElectricObject obj) {}
	public void endChange(ElectricObject obj) {}

	public void modifyNodeInst(com.sun.electric.database.topology.NodeInst ni, double oLX, double oHX, double oLY, double oHY, int oRot, boolean oTrn) {}
	public void modifyNodeInsts(com.sun.electric.database.topology.NodeInst[] nis, double[] oLX, double[] oHX, double[] oLY, double[] oHY, int[] oRot, boolean[] oTrn) {}
	public void modifyArcInst(com.sun.electric.database.topology.ArcInst ai, double oHX, double oHY, double oTX, double oTY, int oWid, double oLen) {}
	public void modifyExport(com.sun.electric.database.hierarchy.Export pp) {}
	public void modifyCell(com.sun.electric.database.hierarchy.Cell cell) {}
	public void modifyTextDescript(ElectricObject obj, int key, Object oldValue) {}

	public void newObject(ElectricObject obj) {}
	public void killObject(ElectricObject obj) {}
	public void newVariable(ElectricObject obj, com.sun.electric.database.variable.Variable.Name key, int type) {}
	public void killVariable(ElectricObject obj, com.sun.electric.database.variable.Variable.Name key, Object oldValue, com.sun.electric.database.variable.TextDescriptor oldDescript) {}
	public void modifyVariable(ElectricObject obj, com.sun.electric.database.variable.Variable.Name key, int type, int index, Object oldValue) {}
	public void insertVariable(ElectricObject obj, com.sun.electric.database.variable.Variable.Name key, int type, int index) {}
	public void deleteVariable(ElectricObject obj, com.sun.electric.database.variable.Variable.Name key, int type, int index, Object oldValue) {}

	public void readLibrary(com.sun.electric.database.hierarchy.Library lib) {}
	public void eraseLibrary(com.sun.electric.database.hierarchy.Library lib) {}
	public void writeLibrary(com.sun.electric.database.hierarchy.Library lib, boolean pass2) {}

}
