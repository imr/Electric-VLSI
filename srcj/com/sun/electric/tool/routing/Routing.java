/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Routing.java
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
package com.sun.electric.tool.routing;

import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * This is the Routing tool.
 */
public class Routing extends Tool
{
	public static class Activity
	{
		int numCreatedArcs, numCreatedNodes;
		ArcInst [] createdArcs;
		NodeInst [] createdNodes;
		int numDeletedArcs, numDeletedNodes;
		ArcInst [] deletedArcs;
		NodeInst [] deletedNodes;
		PortProto [] deletedPorts;

		Activity()
		{
			numCreatedArcs = numCreatedNodes = 0;
			numDeletedArcs = numDeletedNodes = 0;
			createdArcs = new ArcInst[3];
			createdNodes = new NodeInst[3];
			deletedArcs = new ArcInst[3];
			deletedNodes = new NodeInst[2];
			deletedPorts = new PortProto[2];
		}
	}

	private Activity current, past = null;
	private boolean checkAutoStitch = false;

	/** the Routing tool. */		public static Routing tool = new Routing();

	/**
	 * The constructor sets up the Routing tool.
	 */
	private Routing()
	{
		super("Routing");
	}

	/**
	 * Method to initialize the Routing tool.
	 */
	public void init()
	{
		setOn();
	}

	public void startBatch(Tool tool, boolean undoRedo)
	{
		current = new Activity();
		checkAutoStitch = false;
	}

	public void endBatch()
	{
		if (current == null) return;
		if (current.numCreatedArcs > 0 || current.numCreatedNodes > 0 ||
			current.numDeletedArcs > 0 || current.numDeletedNodes > 0)
		{
			past = current;
			if (isMimicStitchOn())
			{
				MimicStitch.mimicStitch(false);
				return;
			}
		}
		if (checkAutoStitch && isAutoStitchOn())
		{
			AutoStitch.autoStitch(false, false);
		}
	}
	public void modifyNodeInst(NodeInst ni, double oCX, double oCY, double oSX, double oSY, int oRot)
	{
		checkAutoStitch = true;
	}
	public void modifyNodeInsts(NodeInst [] nis, double [] oCX, double [] oCY, double [] oSX, double [] oSY, int [] oRot)
	{
		checkAutoStitch = true;
	}

	public void newObject(ElectricObject obj)
	{
		if (obj instanceof NodeInst)
		{
			checkAutoStitch = true;
			if (current.numCreatedNodes < 3)
				current.createdNodes[current.numCreatedNodes++] = (NodeInst)obj;
		} else if (obj instanceof ArcInst)
		{
			if (current.numCreatedArcs < 3)
				current.createdArcs[current.numCreatedArcs++] = (ArcInst)obj;
		}
	}

	public void killObject(ElectricObject obj)
	{
		if (obj instanceof NodeInst)
		{
			if (current.numDeletedNodes < 2)
				current.deletedNodes[current.numDeletedNodes++] = (NodeInst)obj;
		} else if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			if (current.numDeletedArcs < 3)
				current.deletedArcs[current.numDeletedArcs++] = ai;
			current.deletedNodes[0] = ai.getHead().getPortInst().getNodeInst();
			current.deletedPorts[0] = ai.getHead().getPortInst().getPortProto();
			current.deletedNodes[1] = ai.getTail().getPortInst().getNodeInst();
			current.deletedPorts[1] = ai.getTail().getPortInst().getPortProto();
			current.numDeletedNodes = 2;
		}
	}

	/**
	 * Method to return the most recent routing activity.
	 */
	public Activity getLastActivity() { return past; }

	/****************************** OPTIONS ******************************/

	/**
	 * Method to force all Routing Preferences to be saved.
	 */
	private static void flushOptions()
	{
		try
		{
	        tool.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save Routing options");
		}
	}

	/**
	 * Method to tell whether Auto-stitching should be done.
	 * The default is "false".
	 * @return true if Auto-stitching should be done.
	 */
	private static boolean cacheAutoStitchOn = tool.prefs.getBoolean("AutoStitchOn", false);
	public static boolean isAutoStitchOn() { return cacheAutoStitchOn; }
	/**
	 * Method to set whether Auto-stitching should be done.
	 * @param on true if Auto-stitching should be done.
	 */
	public static void setAutoStitchOn(boolean on)
	{
		tool.prefs.putBoolean("AutoStitchOn", cacheAutoStitchOn = on);
		flushOptions();
	}

	/**
	 * Method to tell whether Mimic-stitching should be done.
	 * The default is "false".
	 * @return true if Mimic-stitching should be done.
	 */
	private static boolean cacheMimicStitchOn = tool.prefs.getBoolean("MimicStitchOn", false);
	public static boolean isMimicStitchOn() { return cacheMimicStitchOn; }
	/**
	 * Method to set whether Mimic-stitching should be done.
	 * @param on true if Mimic-stitching should be done.
	 */
	public static void setMimicStitchOn(boolean on)
	{
		tool.prefs.putBoolean("MimicStitchOn", cacheMimicStitchOn = on);
		flushOptions();
	}

	/**
	 * Method to tell whether Mimic-stitching can remove arcs (unstitch).
	 * The default is "false".
	 * @return true if Mimic-stitching can remove arcs (unstitch).
	 */
	private static boolean cacheMimicStitchCanUnstitch = tool.prefs.getBoolean("MimicStitchCanUnstitch", false);
	public static boolean isMimicStitchCanUnstitch() { return cacheMimicStitchCanUnstitch; }
	/**
	 * Method to set whether Mimic-stitching can remove arcs (unstitch).
	 * @param on true if Mimic-stitching can remove arcs (unstitch).
	 */
	public static void setMimicStitchCanUnstitch(boolean on)
	{
		tool.prefs.putBoolean("MimicStitchCanUnstitch", cacheMimicStitchCanUnstitch = on);
		flushOptions();
	}

	/**
	 * Method to tell whether Mimic-stitching runs interactively.
	 * During interactive Mimic stitching, each new set of arcs is shown to the user for confirmation.
	 * The default is "false".
	 * @return true if Mimic-stitching runs interactively.
	 */
	private static boolean cacheMimicStitchInteractive = tool.prefs.getBoolean("MimicStitchInteractive", false);
	public static boolean isMimicStitchInteractive() { return cacheMimicStitchInteractive; }
	/**
	 * Method to set whether Mimic-stitching runs interactively.
	 * During interactive Mimic stitching, each new set of arcs is shown to the user for confirmation.
	 * @param on true if Mimic-stitching runs interactively.
	 */
	public static void setMimicStitchInteractive(boolean on)
	{
		tool.prefs.putBoolean("MimicStitchInteractive", cacheMimicStitchInteractive = on);
		flushOptions();
	}

	/**
	 * Method to tell whether Mimic-stitching only works between matching ports.
	 * The default is "false".
	 * @return true if Mimic-stitching only works between matching ports.
	 */
	private static boolean cacheMimicStitchMatchPorts = tool.prefs.getBoolean("MimicStitchMatchPorts", false);
	public static boolean isMimicStitchMatchPorts() { return cacheMimicStitchMatchPorts; }
	/**
	 * Method to set whether Mimic-stitching only works between matching ports.
	 * @param on true if Mimic-stitching only works between matching ports.
	 */
	public static void setMimicStitchMatchPorts(boolean on)
	{
		tool.prefs.putBoolean("MimicStitchMatchPorts", cacheMimicStitchMatchPorts = on);
		flushOptions();
	}

	/**
	 * Method to tell whether Mimic-stitching only works when the number of existing arcs matches.
	 * The default is "false".
	 * @return true if Mimic-stitching only works when the number of existing arcs matches.
	 */
	private static boolean cacheMimicStitchMatchNumArcs = tool.prefs.getBoolean("MimicStitchMatchNumArcs", false);
	public static boolean isMimicStitchMatchNumArcs() { return cacheMimicStitchMatchNumArcs; }
	/**
	 * Method to set whether Mimic-stitching only works when the number of existing arcs matches.
	 * @param on true if Mimic-stitching only works when the number of existing arcs matches.
	 */
	public static void setMimicStitchMatchNumArcs(boolean on)
	{
		tool.prefs.putBoolean("MimicStitchMatchNumArcs", cacheMimicStitchMatchNumArcs = on);
		flushOptions();
	}

	/**
	 * Method to tell whether Mimic-stitching only works when the node sizes are the same.
	 * The default is "false".
	 * @return true if Mimic-stitching only works when the node sizes are the same.
	 */
	private static boolean cacheMimicStitchMatchNodeSize = tool.prefs.getBoolean("MimicStitchMatchNodeSize", false);
	public static boolean isMimicStitchMatchNodeSize() { return cacheMimicStitchMatchNodeSize; }
	/**
	 * Method to set whether Mimic-stitching only works when the node sizes are the same.
	 * @param on true if Mimic-stitching only works when the node sizes are the same.
	 */
	public static void setMimicStitchMatchNodeSize(boolean on)
	{
		tool.prefs.putBoolean("MimicStitchMatchNodeSize", cacheMimicStitchMatchNodeSize = on);
		flushOptions();
	}

	/**
	 * Method to tell whether Mimic-stitching only works when the nodes have the same type.
	 * The default is "true".
	 * @return true if Mimic-stitching only works when the nodes have the same type.
	 */
	private static boolean cacheMimicStitchMatchNodeType = tool.prefs.getBoolean("MimicStitchMatchNodeType", true);
	public static boolean isMimicStitchMatchNodeType() { return cacheMimicStitchMatchNodeType; }
	/**
	 * Method to set whether Mimic-stitching only works when the nodes have the same type.
	 * @param on true if Mimic-stitching only works when the nodes have the same type.
	 */
	public static void setMimicStitchMatchNodeType(boolean on)
	{
		tool.prefs.putBoolean("MimicStitchMatchNodeType", cacheMimicStitchMatchNodeType = on);
		flushOptions();
	}

	/**
	 * Method to tell whether Mimic-stitching only works when there are no other arcs running in the same direction.
	 * The default is "true".
	 * @return true if Mimic-stitching only works when there are no other arcs running in the same direction.
	 */
	private static boolean cacheMimicStitchNoOtherArcsSameDir = tool.prefs.getBoolean("MimicStitchNoOtherArcsSameDir", true);
	public static boolean isMimicStitchNoOtherArcsSameDir() { return cacheMimicStitchNoOtherArcsSameDir; }
	/**
	 * Method to set whether Mimic-stitching only works when there are no other arcs running in the same direction.
	 * @param on true if Mimic-stitching only works when there are no other arcs running in the same direction.
	 */
	public static void setMimicStitchNoOtherArcsSameDir(boolean on)
	{
		tool.prefs.putBoolean("MimicStitchNoOtherArcsSameDir", cacheMimicStitchNoOtherArcsSameDir = on);
		flushOptions();
	}

	/**
	 * Method to return the name of the arc that should be used as a default by the stitching routers.
	 * The default is "".
	 * @return the name of the arc that should be used as a default by the stitching routers.
	 */
	private static String cachePreferredRoutingArc = tool.prefs.get("PreferredRoutingArc", "");
	public static String getPreferredRoutingArc() { return cachePreferredRoutingArc; }
	/**
	 * Method to set the name of the arc that should be used as a default by the stitching routers.
	 * @param arcName the name of the arc that should be used as a default by the stitching routers.
	 */
	public static void setPreferredRoutingArc(String arcName)
	{
		tool.prefs.put("PreferredRoutingArc", cachePreferredRoutingArc = arcName);
		flushOptions();
	}

}
