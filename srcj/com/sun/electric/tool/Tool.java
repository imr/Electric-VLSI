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

import com.sun.electric.database.change.Changes;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.prefs.Preferences;

/**
 * This class represents a Tool in Electric.  It's here mostly for the name
 * of the tool and the variables attached.  The User holds
 * variables that keep track of the currently selected object, and other
 * useful information.
 */
public class Tool extends ElectricObject implements Changes
{
	// The name of this tool
	private String toolName;
	private int toolState;
	private int toolIndex;

	// the static list of all tools
	private static List tools = new ArrayList();
	private static int toolNumber = 0;

    /** Preferences for this Tool */                        protected Preferences prefs;
    
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
        prefs = Preferences.userNodeForPackage(this.getClass());  // per-package namespace for preferences
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
		Network.tool.init();
        EvalJavaBsh.tool.init();
        // Init LEtool -> must be initialized after EvalJavaBash init() runs, 
        // otherwise Interpreter will be null
        LETool.tool.init();
        Simulation.tool.init();
        Routing.tool.init();
        DRC.tool.init();
	}

	/**
	 * Method to find the Tool with a specified name.
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
	 * Method to return an Iterator over all of the Tools in Electric.
	 * @return an Iterator over all of the Tools in Electric.
	 */
	public static Iterator getTools()
	{
		return tools.iterator();
	}

	/**
	 * Method to return the number of Tools.
	 * @return the number of Tools.
	 */
	public static int getNumTools()
	{
		return tools.size();
	}

	/**
	 * Method to return the name of this Tool.
	 * @return the name of this Tool.
	 */
	public String getName() { return toolName; }

	/**
	 * Method to return the index of this Tool.
	 * Each tool has a 0-based index that can be used to access arrays of Tools.
	 * @return the index of this Tool.
	 */
	public int getIndex() { return toolIndex; }

    /**
     * Method to return Preferences object
     * @return Preferences object
     */
    public Preferences getPrefs() { return prefs; }
    
	/**
	 * Method to set this Tool to be on.
	 * Tools that are "on" are running incrementally, and get slices and broadcasts.
	 */
	public void setOn() { toolState |= TOOLON; }

	/**
	 * Method to set this Tool to be off.
	 * Tools that are "on" are running incrementally, and get slices and broadcasts.
	 */
	public void clearOn() { toolState &= ~TOOLON; }

	/**
	 * Method to tell whether this Tool is on.
	 * Tools that are "on" are running incrementally, and get slices and broadcasts.
	 * @return true if this Tool is on.
	 */
	public boolean isOn() { return (toolState & TOOLON) != 0; }

	/**
	 * Method to set this Tool to be in the background.
	 */
	public void setBackground() { toolState |= TOOLBG; }

	/**
	 * Method to set this Tool to be in the foreground.
	 */
	public void clearBackground() { toolState &= ~TOOLBG; }

	/**
	 * Method to tell whether this Tool is in the background.
	 * @return true if this Tool is in the background.
	 */
	public boolean isBackground() { return (toolState & TOOLBG) != 0; }

	/**
	 * Method to set this Tool to fix errors.
	 */
	public void setFixErrors() { toolState |= TOOLFIX; }

	/**
	 * Method to set this Tool to fix errors.
	 */
	public void clearFixErrors() { toolState &= ~TOOLFIX; }

	/**
	 * Method to tell whether this Tool fixes errors.
	 * @return true if this Tool fixes errors.
	 */
	public boolean isFixErrors() { return (toolState & TOOLFIX) != 0; }

	/**
	 * Method to set this Tool to be incremental.
	 */
	public void setIncremental() { toolState |= TOOLINCREMENTAL; }

	/**
	 * Method to set this Tool to be incremental.
	 */
	public void clearIncremental() { toolState &= ~TOOLINCREMENTAL; }

	/**
	 * Method to tell whether this Tool is incremental.
	 * @return true if this Tool is incremental.
	 */
	public boolean isIncremental() { return (toolState & TOOLINCREMENTAL) != 0; }

	/**
	 * Method to set this Tool to be analysis.
	 */
	public void setAnalysis() { toolState |= TOOLANALYSIS; }

	/**
	 * Method to set this Tool to be analysis.
	 */
	public void clearAnalysis() { toolState &= ~TOOLANALYSIS; }

	/**
	 * Method to tell whether this Tool does analysis.
	 * @return true if this Tool does analysis.
	 */
	public boolean isAnalysis() { return (toolState & TOOLANALYSIS) != 0; }

	/**
	 * Method to set this Tool to be synthesis.
	 */
	public void setSynthesis() { toolState |= TOOLSYNTHESIS; }

	/**
	 * Method to set this Tool to be synthesis.
	 */
	public void clearSynthesis() { toolState &= ~TOOLSYNTHESIS; }

	/**
	 * Method to tell whether this Tool does synthesis.
	 * @return true if this Tool does synthesis.
	 */
	public boolean isSynthesis() { return (toolState & TOOLSYNTHESIS) != 0; }

	/**
	 * Method to determine whether a variable key on Tool is deprecated.
	 * Deprecated variable keys are those that were used in old versions of Electric,
	 * but are no longer valid.
	 * @param key the key of the variable.
	 * @return true if the variable key is deprecated.
	 */
	public boolean isDeprecatedVariable(Variable.Key key)
	{
		String name = key.getName();
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
	public void examineCell(Cell cell) {}
	public void slice() {}

	public void startBatch(Tool tool, boolean undoRedo) {}
	public void endBatch() {}

	public void modifyNodeInst(NodeInst ni, double oCX, double oCY, double oSX, double oSY, int oRot) {}
	public void modifyNodeInsts(NodeInst [] nis, double [] oCX, double [] oCY, double [] oSX, double [] oSY, int [] oRot) {}
	public void modifyArcInst(ArcInst ai, double oHX, double oHY, double oTX, double oTY, double oWid) {}
	public void modifyExport(Export pp, PortInst oldPi) {}
	public void modifyCell(Cell cell, double oLX, double oHX, double oLY, double oHY) {}
	public void modifyTextDescript(ElectricObject obj, TextDescriptor descript, int oldDescript0, int oldDescript1) {}

	public void newObject(ElectricObject obj) {}
	public void killObject(ElectricObject obj) {}
	public void redrawObject(ElectricObject obj) {}
	public void newVariable(ElectricObject obj, Variable var) {}
	public void killVariable(ElectricObject obj, Variable var) {}
	public void modifyVariableFlags(ElectricObject obj, Variable var, int oldFlags) {}
	public void modifyVariable(ElectricObject obj, Variable var, int index, Object oldValue) {}
	public void insertVariable(ElectricObject obj, Variable var, int index) {}
	public void deleteVariable(ElectricObject obj, Variable var, int index, Object oldValue) {}

	public void readLibrary(Library lib) {}
	public void eraseLibrary(Library lib) {}
	public void writeLibrary(Library lib, boolean pass2) {}

}
