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

import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.Highlight;

import java.awt.event.ActionEvent;
import java.util.Iterator;
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
		ArcInst ai = (ArcInst)Highlight.getOneElectricObject(ArcInst.class);
		if (ai == null) return;
		past = new Activity();
		past.createdArcs[past.numCreatedArcs++] = ai;
		MimicStitch.mimicStitch(false);
	}

	/**
	 * Method to convert the current network(s) to an unrouted wire.
	 * The method isn't used yet because there are no routers that use unrouted wires.
	 */
	public static void unrouteCurrent()
	{
		// see what is highlighted
		Set nets = Highlight.getHighlightedNetworks();
		if (nets.size() == 0)
		{
			System.out.println("Must select networks to unroute");
			return;
		}

		// convert requested nets
		Highlight.clear();
		Highlight.finished();
		for(Iterator it = nets.iterator(); it.hasNext(); )
		{
			JNetwork net = (JNetwork)it.next();

			// now unroute the net
			if (ro_unroutenet(net)) return;
		}
	}

	public static boolean ro_unroutenet(JNetwork net)
	{
//		// convert this net and mark arcs and nodes on it
//		count = ro_findnetends(net, &nilist, &pplist, &xlist, &ylist);
//
//		// remove marked nodes and arcs
//		np = net->parent;
//		for(ai = np->firstarcinst; ai != NOARCINST; ai = nextai)
//		{
//			nextai = ai->nextarcinst;
//			if (ai->temp1 == 0) continue;
//			startobjectchange((INTBIG)ai, VARCINST);
//			if (killarcinst(ai))
//			{
//				ttyputerr(_("Error deleting arc"));
//				return(TRUE);
//			}
//		}
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = nextni)
//		{
//			nextni = ni->nextnodeinst;
//			if (ni->temp1 == 0) continue;
//			startobjectchange((INTBIG)ni, VNODEINST);
//			if (killnodeinst(ni))
//			{
//				ttyputerr(_("Error deleting intermediate node"));
//				return(TRUE);
//			}
//		}
//
//		// now create the new unrouted wires
//		bits = us_makearcuserbits(gen_unroutedarc);
//		wid = defaultarcwidth(gen_unroutedarc);
//		covered = (INTBIG *)emalloc(count * SIZEOFINTBIG, el_tempcluster);
//		if (covered == 0) return(FALSE);
//		for(i=0; i<count; i++) covered[i] = 0;
//		for(first=0; ; first++)
//		{
//			found = 1;
//			bestdist = besti = bestj = 0;
//			for(i=0; i<count; i++)
//			{
//				for(j=i+1; j<count; j++)
//				{
//					if (first != 0)
//					{
//						if (covered[i] + covered[j] != 1) continue;
//					}
//					dist = computedistance(xlist[i], ylist[i], xlist[j], ylist[j]);
//
//					// LINTED "bestdist" used in proper order
//					if (found == 0 && dist >= bestdist) continue;
//					found = 0;
//					bestdist = dist;
//					besti = i;
//					bestj = j;
//				}
//			}
//			if (found != 0) break;
//
//			covered[besti] = covered[bestj] = 1;
//			ai = newarcinst(gen_unroutedarc, wid, bits,
//				nilist[besti], pplist[besti], xlist[besti], ylist[besti],
//				nilist[bestj], pplist[bestj], xlist[bestj], ylist[bestj], np);
//			if (ai == NOARCINST)
//			{
//				ttyputerr(_("Could not create unrouted arc"));
//				return(TRUE);
//			}
//			endobjectchange((INTBIG)ai, VARCINST);
//			(void)asktool(us_tool, x_("show-object"), (INTBIG)ai->geom);
//		}
		return false;
	}

	/**
	 * Method to find the endpoints of network "net" and store them in the array
	 * "ni/pp/xp/yp".  Returns the number of nodes in the array.
	 * As a side effect, sets "temp1" on nodes and arcs to nonzero if they are part
	 * of the network.
	 */
//	INTBIG ro_findnetends(NETWORK *net, NODEINST ***nilist, PORTPROTO ***pplist,
//		INTBIG **xplist, INTBIG **yplist)
//	{
//		// initialize
//		np = net->parent;
//		listcount = 0;
//		for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst) ai->temp1 = 0;
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst) ni->temp1 = 0;
//
//		// look at every arc and see if it is part of the network
//		for(ai = np->firstarcinst; ai != NOARCINST; ai = ai->nextarcinst)
//		{
//			if (ai->network != net) continue;
//			ai->temp1 = 1;
//
//			// see if an end of the arc is a network "end"
//			for(i=0; i<2; i++)
//			{
//				ni = ai->end[i].nodeinst;
//				thispi = ai->end[i].portarcinst;
//				term = FALSE;
//				for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//					if (pi != thispi && pi->conarcinst->network == net) break;
//				if (pi == NOPORTARCINST) term = TRUE;
//				if (ni->firstportexpinst != NOPORTEXPINST) term = TRUE;
//				if (ni->proto->primindex == 0) term = TRUE;
//				if (term)
//				{
//					// valid network end: see if it is in the list
//					for(j=0; j<listcount; j++)
//						if (ni == ro_findnetlistni[j] && thispi->proto == ro_findnetlistpp[j])
//							break;
//					if (j < listcount) continue;
//
//					// add it to the list
//					if (listcount >= ro_findnetlisttotal)
//					{
//						newtotal = listcount * 2;
//						if (newtotal == 0) newtotal = 10;
//						newlistni = (NODEINST **)emalloc(newtotal * (sizeof (NODEINST *)),
//							ro_tool->cluster);
//						newlistpp = (PORTPROTO **)emalloc(newtotal * (sizeof (PORTPROTO *)),
//							ro_tool->cluster);
//						newlistx = (INTBIG *)emalloc(newtotal * SIZEOFINTBIG,
//							ro_tool->cluster);
//						newlisty = (INTBIG *)emalloc(newtotal * SIZEOFINTBIG,
//							ro_tool->cluster);
//						for(j=0; j<listcount; j++)
//						{
//							newlistni[j] = ro_findnetlistni[j];
//							newlistpp[j] = ro_findnetlistpp[j];
//							newlistx[j] = ro_findnetlistx[j];
//							newlisty[j] = ro_findnetlisty[j];
//						}
//						if (ro_findnetlisttotal > 0)
//						{
//							efree((CHAR *)ro_findnetlistni);
//							efree((CHAR *)ro_findnetlistpp);
//							efree((CHAR *)ro_findnetlistx);
//							efree((CHAR *)ro_findnetlisty);
//						}
//						ro_findnetlistni = newlistni;
//						ro_findnetlistpp = newlistpp;
//						ro_findnetlistx = newlistx;
//						ro_findnetlisty = newlisty;
//						ro_findnetlisttotal = newtotal;
//					}
//					ro_findnetlistni[listcount] = ni;
//					ro_findnetlistpp[listcount] = thispi->proto;
//					ro_findnetlistx[listcount] = ai->end[i].xpos;
//					ro_findnetlisty[listcount] = ai->end[i].ypos;
//					listcount++;
//				} else
//				{
//					// not a network end: mark the node for removal
//					ni->temp1 = 1;
//				}
//			}
//		}
//		*nilist = ro_findnetlistni;
//		*pplist = ro_findnetlistpp;
//		*xplist = ro_findnetlistx;
//		*yplist = ro_findnetlisty;
//		return(listcount);
//	}

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
