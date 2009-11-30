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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.compaction.Compaction;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.erc.ERC;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.logicaleffort.LETool;
import com.sun.electric.tool.placement.Placement;
import com.sun.electric.tool.project.Project;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.sc.SilComp;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.extract.Extract;
import com.sun.electric.tool.extract.ParasiticTool;
import com.sun.electric.tool.extract.LayerCoverageTool;
import com.sun.electric.tool.generator.layout.GateLayGenSettings;
import com.sun.electric.tool.cvspm.CVS;

import com.sun.electric.tool.generator.layout.fill.FillGeneratorTool;
import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * This class represents a Tool in Electric.  It's here mostly for the name
 * of the tool and the variables attached.  The User holds
 * variables that keep track of the currently selected object, and other
 * useful information.
 */
public class Tool implements Comparable
{
	// The name of this tool
	private String toolName;
	private int toolState;
	private int toolIndex;

	// the static list of all tools
	private static LinkedHashMap<String,Tool> tools = new LinkedHashMap<String,Tool>();
	private static List<Listener> listeners = new ArrayList<Listener>();
	private static int toolNumber = 0;

    /** Setting Group for this Tool */                      private final Setting.Group settings;
	/** Preferences for this Tool */                        public Pref.Group prefs;

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
	protected Tool(String toolName) {
        this(toolName, toolName+"Tool");
    }

	/**
	 * The constructor for Tool is only called by subclasses.
	 * @param toolName the name of this tool.
     * @param settingGroupName name of Setting Group of this Tool
	 */
	protected Tool(String toolName, String settingGroupName)
	{
		this.toolName = toolName;
		this.toolState = 0;
		this.toolIndex = toolNumber++;
		assert findTool(toolName) == null;
		tools.put(toolName, this);
        settings = ToolSettings.getToolSettings(settingGroupName);
        prefs = Pref.groupForPackage(this.getClass());  // per-package namespace for preferences
        for (Setting setting: settings.getSettings())
            assert setting.getPrefPath().equals(prefs.relativePath() + "/" + setting.getPrefName());
	}

	private void updateListeners()
	{
		listeners.clear();
		for (Tool t : tools.values())
		{
			if (t instanceof Listener && ((Listener)t).isOn())
				listeners.add((Listener)t);
		}
	}

	/**
	 * Method called once, at the start of Electric, to initialize the Tools.
	 * Because of Java's "lazy evaluation", the only way to force the Tool constructors to fire
	 * and build a proper list of Tools, each class must somehow be referenced.
	 * So, each Tool is listed here.  If a new Tool is created, this method must be updated.
	 * New tools should also be added to com.sun.electric.tool.ToolSettings.java
	 */
	public static void initAllTools()
	{
		User.getUserTool().init();
		Compaction.getCompactionTool().init();
        DRC.getDRCTool().init();
        ERC.getERCTool().init();
        Extract.getExtractTool().init();
        IOTool.getIOTool().init();
        LETool.getLETool().init();
		NetworkTool.getNetworkTool().init();
        ParasiticTool.getParasiticTool().init();
		Placement.getPlacementTool().init();
        Project.getProjectTool().init();
        Routing.getRoutingTool().init();
        SilComp.getSilCompTool().init();
        Simulation.getSimulationTool().init();
        LayerCoverageTool.getLayerCoverageTool().init();
        GateLayGenSettings.tool.init();
        FillGeneratorTool.getTool().init();
        CVS.getCVSTool().init();
        try {
            Class<?> staClass = Class.forName("com.sun.electric.plugins.sctiming.RunSTA");
            if (staClass != null) {
                Method staInit = staClass.getMethod("staticinit");
                staInit.invoke(Tool.class);
            }
        } catch (Exception e) {
//            System.out.println(e.getMessage());
        }
	}

	/**
	 * Method to find the Tool with a specified name.
	 * @param name the name of the desired Tool.
	 * @return the Tool with that name, or null if no tool matches.
	 */
	public static Tool findTool(String name)
	{
		return tools.get(name);
	}

	/**
	 * Method to return an Iterator over all of the Tools in Electric.
	 * @return an Iterator over all of the Tools in Electric.
	 */
	public static Iterator<Tool> getTools()
	{
		return tools.values().iterator();
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
	 * Method to return an Iterator over all of the Listener in Electric
	 * which are on.
	 * @return an Iterator over all of the Listeners in Electric which are on
	 */
	public static Iterator<Listener> getListeners()
	{
		return listeners.iterator();
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
	 * Method to set this Tool to be on.
	 * Tools that are "on" are running incrementally, and get slices and broadcasts.
	 */
	public void setOn()
	{
		toolState |= TOOLON;
		updateListeners();
	}

	/**
	 * Method to set this Tool to be off.
	 * Tools that are "on" are running incrementally, and get slices and broadcasts.
	 */
	public void clearOn()
	{
		toolState &= ~TOOLON;
		updateListeners();
	}

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

    public Setting.Group getProjectSettings() {
        return settings;
    }

    /**
     * Compares Tools by their definition order.
     * @param obj the other Tool.
     * @return a comparison between the Tools.
     */
	public int compareTo(Object obj)
	{
		Tool that = (Tool)obj;
		return this.toolIndex - that.toolIndex;
	}

	/**
	 * Returns a printable version of this Tool.
	 * @return a printable version of this Tool.
	 */
    @Override
	public String toString()
	{
		return "Tool '" + toolName + "'";
	}

	/**
	 * Method to set a variable on an ElectricObject in a new Job.
	 * @param obj the ElectricObject on which to set the variable.
	 * @param key the Variable key.
	 * @param newVal the new value of the Variable.
	 */
	public void setVarInJob(ElectricObject obj, Variable.Key key, Object newVal)
	{
		new SetVarJob(this, obj, key, newVal);
	}

	/**
	 * Class for setting a variable in a new Job.
	 */
	private static class SetVarJob extends Job
	{
		private ElectricObject obj;
		private Variable.Key key;
		private Object newVal;

		protected SetVarJob(Tool tool, ElectricObject obj, Variable.Key key, Object newVal)
		{
			super("Add Variable", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.obj = obj;
			this.key = key;
			this.newVal = newVal;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			obj.newVar(key, newVal);
			return true;
		}
	}

	/**
	 * The initialization method for this Tool.
	 * Gets overridden by tools that want to do initialization.
	 */
	public void init() {}

    /***********************************
     * Test interface
     ***********************************/
    public static boolean testAll()
    {
        return true; // nothing to test
    }
}
