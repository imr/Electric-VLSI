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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;

/**
 * This is the Routing tool.
 */
public class Routing extends Listener
{
	/**
	 * Class to describe recent activity that pertains to routing.
	 */
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

	/****************************** TOOL INTERFACE ******************************/

	/**
	 * The constructor sets up the Routing tool.
	 */
	private Routing()
	{
		super("routing");
	}

	/**
	 * Method to initialize the Routing tool.
	 */
	public void init()
	{
		setOn();
	}

	/**
	 * Method to announce the start of a batch of changes.
	 * @param tool the tool that generated the changes.
	 * @param undoRedo true if these changes are from an undo or redo command.
	 */
	public void startBatch(Tool tool, boolean undoRedo)
	{
		current = new Activity();
		checkAutoStitch = false;
	}

	/**
	 * Method to announce the end of a batch of changes.
	 */
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

	/**
	 * Method to announce a change to a NodeInst.
	 * @param ni the NodeInst that was changed.
	 * @param oCX the old X center of the NodeInst.
	 * @param oCY the old Y center of the NodeInst.
	 * @param oSX the old X size of the NodeInst.
	 * @param oSY the old Y size of the NodeInst.
	 * @param oRot the old rotation of the NodeInst.
	 */
	public void modifyNodeInst(NodeInst ni, double oCX, double oCY, double oSX, double oSY, int oRot)
	{
		checkAutoStitch = true;
	}

	/**
	 * Method to announce a change to many NodeInsts at once.
	 * @param nis the NodeInsts that were changed.
	 * @param oCX the old X centers of the NodeInsts.
	 * @param oCY the old Y centers of the NodeInsts.
	 * @param oSX the old X sizes of the NodeInsts.
	 * @param oSY the old Y sizes of the NodeInsts.
	 * @param oRot the old rotations of the NodeInsts.
	 */
	public void modifyNodeInsts(NodeInst [] nis, double [] oCX, double [] oCY, double [] oSX, double [] oSY, int [] oRot)
	{
		checkAutoStitch = true;
	}

	/**
	 * Method to announce the creation of a new ElectricObject.
	 * @param obj the ElectricObject that was just created.
	 */
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

	/**
	 * Method to announce the deletion of an ElectricObject.
	 * @param obj the ElectricObject that was just deleted.
	 */
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

	/****************************** COMMANDS ******************************/

	/**
	 * Method to mimic the currently selected ArcInst.
	 */
	public void mimicSelected()
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		ArcInst ai = (ArcInst)highlighter.getOneElectricObject(ArcInst.class);
		if (ai == null) return;
		past = new Activity();
		past.createdArcs[past.numCreatedArcs++] = ai;
		MimicStitch.mimicStitch(false);
	}

	/**
	 * Method to convert the current network(s) to an unrouted wire.
	 */
	public static void unrouteCurrent()
	{
		UnrouteJob job = new UnrouteJob();
	}

	private static class UnrouteJob extends Job
	{
		protected UnrouteJob()
		{
			super("Unroute", Routing.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

		public boolean doIt()
		{
			// see what is highlighted
	        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
	        if (wf == null) return false;
	        Highlighter highlighter = wf.getContent().getHighlighter();
	        if (highlighter == null) return false;
			Set nets = highlighter.getHighlightedNetworks();
			if (nets.size() == 0)
			{
				System.out.println("Must select networks to unroute");
				return false;
			}

			// convert requested nets
			Cell cell = wf.getContent().getCell();
			Netlist netList = cell.getUserNetlist();
			highlighter.clear();

			// make arrays of what to unroute
			int total = nets.size();
			Network [] netsToUnroute = new Network[total];
			List [] netEnds = new List[total];
			HashSet [] arcsToDelete = new HashSet[total];
			HashSet [] nodesToDelete = new HashSet[total];
			int i = 0;
			for(Iterator it = nets.iterator(); it.hasNext(); )
			{
				Network net = (Network)it.next();
				netsToUnroute[i] = net;
				arcsToDelete[i] = new HashSet();
				nodesToDelete[i] = new HashSet();
				netEnds[i] = findNetEnds(net, arcsToDelete[i], nodesToDelete[i], netList);
				i++;
			}

			// do the unrouting
			for(int j=0; j<total; j++)
			{
				if (unrouteNet(netsToUnroute[j], arcsToDelete[j], nodesToDelete[j], netEnds[j], netList, highlighter)) return false;
			}
			highlighter.finished();
			return true;
		}

		private static boolean unrouteNet(Network net, HashSet arcsToDelete, HashSet nodesToDelete, List netEnds, Netlist netList, Highlighter highlighter)
		{
			// remove marked nodes and arcs
			for(Iterator it = arcsToDelete.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.kill();
			}
			for(Iterator it = nodesToDelete.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni.kill();
			}

			// now create the new unrouted wires
			double wid = Generic.tech.unrouted_arc.getDefaultWidth();
			int count = netEnds.size();
			int [] covered = new int[count];
			Point2D [] points = new Point2D[count];
			for(int i=0; i<count; i++)
			{
				PortInst pi = (PortInst)netEnds.get(i);
				Poly poly = pi.getPoly();
				points[i] = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
				covered[i] = 0;
			}
			for(int first=0; ; first++)
			{
				boolean found = true;
				double bestdist = 0;
				int besti = 0, bestj = 0;
				for(int i=0; i<count; i++)
				{
					for(int j=i+1; j<count; j++)
					{
						if (first != 0)
						{
							if (covered[i] + covered[j] != 1) continue;
						}
						double dist = points[i].distance(points[j]);
	
						if (!found && dist >= bestdist) continue;
						found = false;
						bestdist = dist;
						besti = i;
						bestj = j;
					}
				}
				if (found) break;

				covered[besti] = covered[bestj] = 1;
				PortInst head = (PortInst)netEnds.get(besti);
				PortInst tail = (PortInst)netEnds.get(bestj);
				ArcInst ai = ArcInst.makeInstance(Generic.tech.unrouted_arc, wid, head, tail);
				if (ai == null)
				{
					System.out.println("Could not create unrouted arc");
					return true;
				}
				highlighter.addElectricObject(ai, ai.getParent());
			}
			return false;
		}
	}

	/**
	 * Method to find the endpoints of a network.
	 * @param net the network to "unroute".
	 * @param arcsToDelete a HashSet of arcs that should be deleted.
	 * @param nodesToDelete a HashSet of nodes that should be deleted.
	 * @param netList the netlist for the current cell.
	 * @return a List of PortInsts that should be wired together.
	 */
	public static List findNetEnds(Network net, HashSet arcsToDelete, HashSet nodesToDelete, Netlist netList)
	{
		// initialize
		Cell cell = net.getParent();
		List endList = new ArrayList();

		// look at every arc and see if it is part of the network
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			Network aNet = netList.getNetwork(ai, 0);
			if (aNet != net) continue;
			arcsToDelete.add(ai);

			// see if an end of the arc is a network "end"
			for(int i=0; i<2; i++)
			{
				Connection thisCon = ai.getConnection(i);
				NodeInst ni = thisCon.getPortInst().getNodeInst();
				boolean term = true;
				for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = (Connection)cIt.next();
					if (con != thisCon && netList.getNetwork(con.getArc(), 0) == net) { term = false;   break; }
				}
				if (ni.getNumExports() > 0) term = true;
				if (ni.getProto() instanceof Cell) term = true;
				if (term)
				{
					// valid network end: see if it is in the list
					if (!endList.contains(thisCon.getPortInst()))
						endList.add(thisCon.getPortInst());
				} else
				{
					// not a network end: mark the node for removal
					nodesToDelete.add(ni);
				}
			}
		}
		return endList;
	}

	/**
	 * Method to return the most recent routing activity.
	 */
	public Activity getLastActivity() { return past; }

	/**
	 * Method called when the "Enable Auto Stitching" command is issued.
	 * Toggles the state of automatic auto stitching.
	 * @param e the event with the menu item that issued the command.
	 */
	public static void toggleEnableAutoStitching(ActionEvent e)
	{
		AbstractButton b = (AbstractButton)e.getSource();
		if (b.isSelected())
		{
			setAutoStitchOn(true);
			System.out.println("Auto-stitching enabled");
		} else
		{
			setAutoStitchOn(false);
			System.out.println("Auto-stitching disabled");
		}
	}

	/**
	 * Method called when the "Enable Mimic Stitching" command is issued.
	 * Toggles the state of automatic mimic stitching.
	 * @param e the event with the menu item that issued the command.
	 */
	public static void toggleEnableMimicStitching(ActionEvent e)
	{
		AbstractButton b = (AbstractButton)e.getSource();
		if (b.isSelected())
		{
			setMimicStitchOn(true);
			System.out.println("Mimic-stitching enabled");
		} else
		{
			setMimicStitchOn(false);
			System.out.println("Mimic-stitching disabled");
		}
	}

	/****************************** OPTIONS ******************************/

	private static Pref cacheAutoStitchOn = Pref.makeBooleanPref("AutoStitchOn", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Auto-stitching should be done.
	 * The default is "false".
	 * @return true if Auto-stitching should be done.
	 */
	public static boolean isAutoStitchOn() { return cacheAutoStitchOn.getBoolean(); }
	/**
	 * Method to set whether Auto-stitching should be done.
	 * @param on true if Auto-stitching should be done.
	 */
	public static void setAutoStitchOn(boolean on) { cacheAutoStitchOn.setBoolean(on); }

	private static Pref cacheMimicStitchOn = Pref.makeBooleanPref("MimicStitchOn", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching should be done.
	 * The default is "false".
	 * @return true if Mimic-stitching should be done.
	 */
	public static boolean isMimicStitchOn() { return cacheMimicStitchOn.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching should be done.
	 * @param on true if Mimic-stitching should be done.
	 */
	public static void setMimicStitchOn(boolean on) { cacheMimicStitchOn.setBoolean(on); }

	private static Pref cacheMimicStitchCanUnstitch = Pref.makeBooleanPref("MimicStitchCanUnstitch", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching can remove arcs (unstitch).
	 * The default is "false".
	 * @return true if Mimic-stitching can remove arcs (unstitch).
	 */
	public static boolean isMimicStitchCanUnstitch() { return cacheMimicStitchCanUnstitch.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching can remove arcs (unstitch).
	 * @param on true if Mimic-stitching can remove arcs (unstitch).
	 */
	public static void setMimicStitchCanUnstitch(boolean on) { cacheMimicStitchCanUnstitch.setBoolean(on); }

	private static Pref cacheMimicStitchInteractive = Pref.makeBooleanPref("MimicStitchInteractive", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching runs interactively.
	 * During interactive Mimic stitching, each new set of arcs is shown to the user for confirmation.
	 * The default is "false".
	 * @return true if Mimic-stitching runs interactively.
	 */
	public static boolean isMimicStitchInteractive() { return cacheMimicStitchInteractive.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching runs interactively.
	 * During interactive Mimic stitching, each new set of arcs is shown to the user for confirmation.
	 * @param on true if Mimic-stitching runs interactively.
	 */
	public static void setMimicStitchInteractive(boolean on) { cacheMimicStitchInteractive.setBoolean(on); }

	private static Pref cacheMimicStitchMatchPorts = Pref.makeBooleanPref("MimicStitchMatchPorts", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching only works between matching ports.
	 * The default is "false".
	 * @return true if Mimic-stitching only works between matching ports.
	 */
	public static boolean isMimicStitchMatchPorts() { return cacheMimicStitchMatchPorts.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works between matching ports.
	 * @param on true if Mimic-stitching only works between matching ports.
	 */
	public static void setMimicStitchMatchPorts(boolean on) { cacheMimicStitchMatchPorts.setBoolean(on); }

	private static Pref cacheMimicStitchMatchNumArcs = Pref.makeBooleanPref("MimicStitchMatchNumArcs", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching only works when the number of existing arcs matches.
	 * The default is "false".
	 * @return true if Mimic-stitching only works when the number of existing arcs matches.
	 */
	public static boolean isMimicStitchMatchNumArcs() { return cacheMimicStitchMatchNumArcs.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works when the number of existing arcs matches.
	 * @param on true if Mimic-stitching only works when the number of existing arcs matches.
	 */
	public static void setMimicStitchMatchNumArcs(boolean on) { cacheMimicStitchMatchNumArcs.setBoolean(on); }

	private static Pref cacheMimicStitchMatchNodeSize = Pref.makeBooleanPref("MimicStitchMatchNodeSize", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching only works when the node sizes are the same.
	 * The default is "false".
	 * @return true if Mimic-stitching only works when the node sizes are the same.
	 */
	public static boolean isMimicStitchMatchNodeSize() { return cacheMimicStitchMatchNodeSize.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works when the node sizes are the same.
	 * @param on true if Mimic-stitching only works when the node sizes are the same.
	 */
	public static void setMimicStitchMatchNodeSize(boolean on) { cacheMimicStitchMatchNodeSize.setBoolean(on); }

	private static Pref cacheMimicStitchMatchNodeType = Pref.makeBooleanPref("MimicStitchMatchNodeType", Routing.tool.prefs, true);
	/**
	 * Method to tell whether Mimic-stitching only works when the nodes have the same type.
	 * The default is "true".
	 * @return true if Mimic-stitching only works when the nodes have the same type.
	 */
	public static boolean isMimicStitchMatchNodeType() { return cacheMimicStitchMatchNodeType.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works when the nodes have the same type.
	 * @param on true if Mimic-stitching only works when the nodes have the same type.
	 */
	public static void setMimicStitchMatchNodeType(boolean on) { cacheMimicStitchMatchNodeType.setBoolean(on); }

	private static Pref cacheMimicStitchNoOtherArcsSameDir = Pref.makeBooleanPref("MimicStitchNoOtherArcsSameDir", Routing.tool.prefs, true);
	/**
	 * Method to tell whether Mimic-stitching only works when there are no other arcs running in the same direction.
	 * The default is "true".
	 * @return true if Mimic-stitching only works when there are no other arcs running in the same direction.
	 */
	public static boolean isMimicStitchNoOtherArcsSameDir() { return cacheMimicStitchNoOtherArcsSameDir.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works when there are no other arcs running in the same direction.
	 * @param on true if Mimic-stitching only works when there are no other arcs running in the same direction.
	 */
	public static void setMimicStitchNoOtherArcsSameDir(boolean on) { cacheMimicStitchNoOtherArcsSameDir.setBoolean(on); }

	private static Pref cachePreferredRoutingArc = Pref.makeStringPref("PreferredRoutingArc", Routing.tool.prefs, "");
	/**
	 * Method to return the name of the arc that should be used as a default by the stitching routers.
	 * The default is "".
	 * @return the name of the arc that should be used as a default by the stitching routers.
	 */
	public static String getPreferredRoutingArc() { return cachePreferredRoutingArc.getString(); }
	/**
	 * Method to set the name of the arc that should be used as a default by the stitching routers.
	 * @param arcName the name of the arc that should be used as a default by the stitching routers.
	 */
	public static void setPreferredRoutingArc(String arcName) { cachePreferredRoutingArc.setString(arcName); }

}
