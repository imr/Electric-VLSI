/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Clipboard.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.MenuCommands;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.ui.EditWindow;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/*
 * Class for managing the circuitry clipboard (for copy and paste).
 */
public class Clipboard
{
	/** The only Clipboard object. */					private static Clipboard theClipboard = new Clipboard();
	/** The Clipboard Library. */						private static Library clipLib = null;
	/** The Clipboard Cell. */							private static Cell clipCell;

	/**
	 * The constructor gets called only once.
	 * It creates the clipboard Library and Cell.
	 */
	private Clipboard()
	{
	}

	private static void init()
	{
		if (clipLib != null) return;
		clipLib = Library.newInstance("Clipboard!!", null);
		clipLib.setHidden();
		clipCell = Cell.newInstance(clipLib, "Clipboard!!");
	}

	/**
	 * Method to clear the contents of the clipboard.
	 */
	public static void clear()
	{
		init();

		// delete all arcs in the clipboard
		List arcsToDelete = new ArrayList();
		for(Iterator it = clipCell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			arcsToDelete.add(ai);
		}
		for(Iterator it = arcsToDelete.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			ai.kill();
		}

		// delete all exports in the clipboard
		List exportsToDelete = new ArrayList();
		for(Iterator it = clipCell.getPorts(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			exportsToDelete.add(pp);
		}
		for(Iterator it = exportsToDelete.iterator(); it.hasNext(); )
		{
			Export pp = (Export)it.next();
			pp.kill();
		}

		// delete all nodes in the clipboard
		List nodesToDelete = new ArrayList();
		for(Iterator it = clipCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			nodesToDelete.add(ni);
		}
		for(Iterator it = nodesToDelete.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			ni.kill();
		}
	}

	public static void copy()
	{
        CopyObjects job = new CopyObjects();
	}

	protected static class CopyObjects extends Job
	{
		protected CopyObjects()
		{
			super("Copy", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			// get objects to copy
			List geoms = Highlight.getHighlighted(true, true);
			if (geoms.size() == 0)
			{
				System.out.println("First select objects to copy");
				return;
			}

			// determine the cell with these geometrics
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd == null) return;
			Cell parent = wnd.getCell();

			// remove contents of clipboard
			clear();

			// copy objects to clipboard
	//		saveview = us_clipboardcell->cellview;
	//		us_clipboardcell->cellview = parent->cellview;
			copyListToCell(wnd, geoms, parent, clipCell, false, false, false);
	//		us_clipboardcell->cellview = saveview;
		}
	}

	public static void cut()
	{
        CutObjects job = new CutObjects();
	}

	protected static class CutObjects extends Job
	{
		protected CutObjects()
		{
			super("Cut", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			// get objects to cut
			List geoms = Highlight.getHighlighted(true, true);
			if (geoms.size() == 0)
			{
				System.out.println("First select objects to cut");
				return;
			}

			// determine the cell with these geometrics
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd == null) return;
			Cell parent = wnd.getCell();

			// remove contents of clipboard
			clear();

			// copy objects to clipboard
	//		saveview = us_clipboardcell->cellview;
	//		us_clipboardcell->cellview = parent->cellview;
			copyListToCell(wnd, geoms, parent, clipCell, false, false, false);
	//		us_clipboardcell->cellview = saveview;

			// and delete the original objects
			CircuitChanges.eraseObjectsInList(parent, geoms);
		}
	}

	public static void paste()
	{
        PasteObjects job = new PasteObjects();
	}

	protected static class PasteObjects extends Job
	{
		protected PasteObjects()
		{
			super("Paste", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.startJob();
		}

		public void doIt()
		{
			// get objects to paste
			Clipboard.init();
			int ntotal = clipCell.getNumNodes();
			int atotal = clipCell.getNumArcs();
			int total = ntotal + atotal;
			if (total == 0)
			{
				System.out.println("Nothing in the clipboard to paste");
				return;
			}

			// find out where the paste is going
			EditWindow wnd = EditWindow.getCurrent();
			if (wnd == null) return;
			Cell parent = wnd.getCell();
			// special case of pasting on top of selected objects
			List geoms = Highlight.getHighlighted(true, true);
			if (geoms.size() > 0)
			{
				// can only paste a single object onto selection
				if (ntotal == 2 && atotal == 1)
				{
					ArcInst ai = (ArcInst)clipCell.getArcs().next();
					NodeInst niHead = ai.getHead().getPortInst().getNodeInst();
					NodeInst niTail = ai.getTail().getPortInst().getNodeInst();
					Iterator nIt = clipCell.getNodes();
					NodeInst ni1 = (NodeInst)nIt.next();
					NodeInst ni2 = (NodeInst)nIt.next();
					if ((ni1 == niHead && ni2 == niTail) ||
						(ni1 == niTail && ni2 == niHead)) ntotal = 0;
					total = ntotal + atotal;
				}
				if (total > 1)
				{
					System.out.println("Can only paste a single object on top of selected objects");
					return;
				}
				Highlight.clear();
				Highlight.finished();
				boolean overlaid = false;
				for(Iterator it = geoms.iterator(); it.hasNext(); )
				{
					Geometric geom = (Geometric)it.next();
					if (geom instanceof NodeInst && ntotal == 1)
					{
						NodeInst ni = (NodeInst)geom;
						NodeInst firstInClip = (NodeInst)clipCell.getNodes().next();
						ni = pasteNodeToNode(ni, firstInClip);
						if (ni != null) overlaid = true;
					} else if (geom instanceof ArcInst && atotal == 1)
					{
						ArcInst ai = (ArcInst)geom;
						ai = pasteArcToArc(ai, (ArcInst)clipCell.getArcs().next());
						if (ai != null) overlaid = true;
					}
				}
				if (!overlaid)
					System.out.println("Nothing was pasted");
				return;
			}

			List pasteList = new ArrayList();
			for(Iterator it = clipCell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				pasteList.add(ni);
			}
			for(Iterator it = clipCell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				pasteList.add(ai);
			}

			// paste them into the current cell
			boolean interactiveplace = true;
			copyListToCell(wnd, pasteList, clipCell, wnd.getCell(), true, interactiveplace, false);
		}
	}

	/**
	 * Returns a printable version of this Clipboard.
	 * @return a printable version of this Clipboard.
	 */
	public String toString() { return "Clipboard"; }

	private static boolean dupDistSet = false;
	private static double dupX, dupY;

	static class NodeNameCaseInsensitive implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			NodeInst n1 = (NodeInst)o1;
			NodeInst n2 = (NodeInst)o2;
			String s1 = n1.getName();
			String s2 = n1.getName();
			if (s1 == null) s1 = "";
			if (s2 == null) s2 = "";
			return s1.compareToIgnoreCase(s2);
		}
	}

	static class ArcNameCaseInsensitive implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			ArcInst a1 = (ArcInst)o1;
			ArcInst a2 = (ArcInst)o2;
			String s1 = a1.getName();
			String s2 = a1.getName();
			if (s1 == null) s1 = "";
			if (s2 == null) s2 = "";
			return s1.compareToIgnoreCase(s2);
		}
	}

	static class ExportNameCaseInsensitive implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Export e1 = (Export)o1;
			Export e2 = (Export)o2;
			String s1 = e1.getProtoName();
			String s2 = e1.getProtoName();
			if (s1 == null) s1 = "";
			if (s2 == null) s2 = "";
			return s1.compareToIgnoreCase(s2);
		}
	}

	/**
	 * Method to copy the list of objects in "list" (NOGEOM terminated) from "fromcell"
	 * to "tocell".  If "highlight" is true, highlight the objects in the new cell.
	 * If "interactiveplace" is true, interactively select the location in the new cell.
	 */
	private static void copyListToCell(EditWindow wnd, List list, Cell fromcell, Cell tocell, boolean highlight,
		boolean interactiveplace, boolean showoffset)
	{
		// make sure the destination cell can be modified
		if (CircuitChanges.cantEdit(tocell, null, true)) return;

		// make sure they are all in the same cell
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (fromcell != geom.getParent())
			{
				System.out.println("All duplicated objects must be in the same cell");
				return;
			}
		}

		// set the technology of the new cell from the old cell if the new is empty
//		if (tocell->firstnodeinst == NONODEINST ||
//			(tocell->firstnodeinst->proto == gen_cellcenterprim &&
//				tocell->firstnodeinst->nextnodeinst == NONODEINST))
//		{
//			tocell->tech = fromcell->tech;
//		}

		// mark all nodes (including those touched by highlighted arcs)
		FlagSet inAreaFlag = Geometric.getFlagSet(1);
		for(Iterator it = fromcell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			ni.clearBit(inAreaFlag);
		}
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst) continue;
			ArcInst ai = (ArcInst)geom;
			ai.getHead().getPortInst().getNodeInst().setBit(inAreaFlag);
			ai.getTail().getPortInst().getNodeInst().setBit(inAreaFlag);
		}
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof ArcInst) continue;
			NodeInst ni = (NodeInst)geom;

			// check for cell instance lock
			if (ni.getProto() instanceof Cell && tocell.isInstancesLocked())
			{
				if (CircuitChanges.cantEdit(tocell, ni, true)) continue;
			}
			ni.setBit(inAreaFlag);
		}

		// build a list that includes all nodes touching copied arcs
		List theNodes = new ArrayList();
		for(Iterator it = fromcell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.isBit(inAreaFlag)) theNodes.add(ni);
		}
		if (theNodes.size() == 0) return;
		inAreaFlag.freeFlagSet();

		// check for recursion
		for(Iterator it = theNodes.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() instanceof PrimitiveNode) continue;
//			if (isachildof(tocell, ni.getProto()))
//			{
//				System.out.println("Cannot: that would be recursive");
//				return;
//			}
		}

		// figure out lower-left corner of this collection of objects
		Iterator nit = theNodes.iterator();
		NodeInst niFirst = (NodeInst)nit.next();
		Point2D corner = niFirst.getLowLeft();
		for(; nit.hasNext(); )
		{
			NodeInst ni = (NodeInst)nit.next();
			Point2D pt = ni.getLowLeft();
			if (pt.getX() < corner.getY()) corner.setLocation(pt.getX(), corner.getY());
			if (pt.getY() < corner.getY()) corner.setLocation(corner.getX(), pt.getY());
		}
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst) continue;
			ArcInst ai = (ArcInst)geom;

			double wid = ai.getWidth() - ai.getProto().getWidthOffset();
			Poly poly = ai.makePoly(ai.getLength(), wid, Poly.Type.FILLED);
			Rectangle2D bounds = poly.getBounds2D();
			if (bounds.getMinX() < corner.getY()) corner.setLocation(bounds.getMinX(), corner.getY());
			if (bounds.getMinY() < corner.getY()) corner.setLocation(corner.getX(), bounds.getMinY());
		}

		// adjust this corner so that, after grid alignment, objects are in the same location
		EditWindow.gridAlign(corner);

		// special case when moving one node: account for cell center
		if (theNodes.size() == 1 && list.size() == 1)
		{
//			if ((us_useroptions&CENTEREDPRIMITIVES) != 0) centeredprimitives = TRUE; else
//				centeredprimitives = FALSE;
//			corneroffset(niFirst, niFirst->proto, niFirst->rotation, niFirst->transpose, &lx, &ly,
//				centeredprimitives);
//			bestlx = niFirst->lowx + lx;
//			bestly = niFirst->lowy + ly;
//
//			if (niFirst->proto->primindex != 0 && (us_useroptions&CENTEREDPRIMITIVES) == 0)
//			{
//				// adjust this corner so that, after grid alignment, objects are in the same location
//				gridalign(&bestlx, &bestly, 1, tocell);
//			}
		}

		// remove highlighting if planning to highlight new stuff
//		if (highlight) Highlight.clear();
		if (!dupDistSet)
		{
			dupDistSet = true;
			dupX = dupY = 10;
		}
		double dx = dupX;
		double dy = dupY;
//		if (interactiveplace)
//		{
//			// adjust the cursor position if selecting interactively
//			if ((us_tool->toolstate&INTERACTIVE) != 0)
//			{
//				var = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER, us_interactiveanglekey);
//				if (var == NOVARIABLE) angle = 0; else
//					angle = var->addr;
//				us_multidraginit(bestlx, bestly, list, nodelist, total, angle, showoffset);
//				trackcursor(FALSE, us_ignoreup, us_multidragbegin, us_multidragdown,
//					us_stoponchar, us_multidragup, TRACKDRAGGING);
//				if (el_pleasestop != 0)
//				{
//					efree((CHAR *)nodelist);
//					return;
//				}
//				if (us_demandxy(&xcur, &ycur))
//				{
//					efree((CHAR *)nodelist);
//					return;
//				}
//				bits = getbuckybits();
//				if ((bits&CONTROLDOWN) != 0)
//					us_getslide(angle, bestlx, bestly, xcur, ycur, &xcur, &ycur);
//			} else
//			{
//				// get aligned cursor co-ordinates
//				if (us_demandxy(&xcur, &ycur))
//				{
//					efree((CHAR *)nodelist);
//					return;
//				}
//			}
//			gridalign(&xcur, &ycur, 1, tocell);
//
//			dx = xcur-bestlx;
//			dy = ycur-bestly;
//		}

		// initialize for queueing creation of new exports
		List queuedExports = new ArrayList();

		// sort the nodes by name
		Collections.sort(theNodes, new NodeNameCaseInsensitive());

		// create the new objects
		for(Iterator it = theNodes.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			double width = ni.getXSize();
			if (ni.isXMirrored()) width = -width;
			double height = ni.getYSize();
			if (ni.isYMirrored()) height = -height;
			String name = null;
			if (ni.isUsernamed())
				name = ElectricObject.uniqueObjectName(ni.getName(), tocell, NodeInst.class);
			NodeInst newNi = NodeInst.newInstance(ni.getProto(),
				new Point2D.Double(ni.getGrabCenterX()+dx, ni.getGrabCenterY()+dy),
					width, height, ni.getAngle(), tocell, name);
			if (newNi == null)
			{
				System.out.println("Cannot create node");
				return;
			}
			newNi.copyStateBits(ni);
			newNi.clearWiped();
			newNi.clearShortened();
			newNi.setProtoTextDescriptor(ni.getProtoTextDescriptor());
			newNi.setNameTextDescriptor(ni.getNameTextDescriptor());
			newNi.copyVars(ni);
			ni.setTempObj(newNi);
//			us_dupnode = newNi;

			// copy the ports, too
			if (User.isDupCopiesExports())
			{
				for(Iterator eit = ni.getExports(); eit.hasNext(); )
				{
					Export pp = (Export)eit.next();
					queuedExports.add(pp);
				}
			}
		}

		// create any queued exports
		createQueuedExports(queuedExports);

		// create a list of arcs to be copied
		List theArcs = new ArrayList();
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof ArcInst)
				theArcs.add(geom);
		}
		if (theArcs.size() > 0)
		{
			// sort the arcs by name
			Collections.sort(theArcs, new ArcNameCaseInsensitive());

			for(Iterator it = theArcs.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				PortInst oldHeadPi = ai.getHead().getPortInst();
				NodeInst headNi = (NodeInst)oldHeadPi.getNodeInst().getTempObj();
				PortInst headPi = headNi.findPortInstFromProto(oldHeadPi.getPortProto());

				PortInst oldTailPi = ai.getTail().getPortInst();
				NodeInst tailNi = (NodeInst)oldTailPi.getNodeInst().getTempObj();
				PortInst tailPi = tailNi.findPortInstFromProto(oldTailPi.getPortProto());

				String name = null;
				if (ai.isUsernamed())
					name = ElectricObject.uniqueObjectName(ai.getName(), tocell, ArcInst.class);
				ArcInst newAr = ArcInst.newInstance(ai.getProto(), ai.getWidth(),
					headPi, new Point2D.Double(ai.getHead().getLocation().getX() + dx, ai.getHead().getLocation().getY() + dy),
					tailPi, new Point2D.Double(ai.getTail().getLocation().getX() + dx, ai.getTail().getLocation().getY() + dy), name);
				if (newAr == null)
				{
					System.out.println("Cannot create arc");
					return;
				}
				newAr.copyStateBits(ai);
				newAr.copyVars(ai);
				newAr.setNameTextDescriptor(ai.getNameTextDescriptor());
				ai.setTempObj(newAr);
			}
		}

		// highlight the copy
		if (highlight)
		{
			Highlight.clear();
			for(Iterator it = theNodes.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni = (NodeInst)ni.getTempObj();

				// special case for displayable text on invisible pins
				if (ni.isInvisiblePinWithText())
				{
					Poly [] polys = ni.getAllText(false, wnd);
					if (polys == null) continue;
					for(int i=0; i<polys.length; i++)
					{
						Poly poly = polys[i];
						Highlight h = Highlight.addText(ni, tocell, poly.getVariable(), poly.getName());
					}
					continue;
				}
				Highlight h = Highlight.addElectricObject(ni, tocell);
			}
			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (geom instanceof NodeInst) continue;
				ArcInst ai = (ArcInst)geom;
				ai = (ArcInst)ai.getTempObj();
				Highlight h = Highlight.addElectricObject(ai, tocell);
			}
			Highlight.finished();
		}

		// cleanup temp object pointers that correspond from old cell to new
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			geom.setTempObj(null);
		}
		for(Iterator it = theNodes.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			ni.setTempObj(null);
		}
	}

	/**
	 * Method to queue the creation of an export from port "pp" of node "ni".
	 * The port is being copied from an original port "origpp".  Returns true on error.
	 */
	public static void createQueuedExports(List queuedExports)
	{
		/* sort the ports by their original name */
		Collections.sort(queuedExports, new ExportNameCaseInsensitive());

		for(Iterator it = queuedExports.iterator(); it.hasNext(); )
		{
			Export origpp = (Export)it.next();
			PortInst pi = origpp.getOriginalPort();
			NodeInst ni = pi.getNodeInst();
			ni = (NodeInst)ni.getTempObj();

			PortProto pp = pi.getPortProto();
			PortInst newPi = ni.findPortInstFromProto(pp);

			Cell cell = ni.getParent();
			String portname = ElectricObject.uniqueObjectName(origpp.getProtoName(), cell, PortProto.class);
			Export newpp =  Export.newInstance(cell, newPi, portname);
			if (newpp == null) return;
			newpp.setTextDescriptor(origpp.getTextDescriptor());
			newpp.copyVars(origpp);
		}
	}

	/**
	 * Method to "paste" node "srcnode" onto node "destnode", making them the same.
	 * Returns the address of the destination node (NONODEINST on error).
	 */
	private static NodeInst pasteNodeToNode(NodeInst destNode, NodeInst srcNode)
	{
		// if they do not have the same type, replace one with the other
		if (destNode.getProto() != srcNode.getProto())
		{
			destNode = CircuitChanges.replaceNodeInst(destNode, srcNode.getProto(), true, false);
			return destNode;
		}

		// make the sizes the same if they are primitives
		if (destNode.getProto() instanceof PrimitiveNode)
		{
			double dx = srcNode.getXSize() - destNode.getXSize();
			double dy = srcNode.getYSize() - destNode.getYSize();
			if (dx != 0 || dy != 0)
			{
				double dlx = -dx/2;   double dhx = dx/2;
				double dly = -dy/2;   double dhy = dy/2;
				destNode.modifyInstance(dlx, dly, 0, 0, 0);
			}
		}

		// remove variables that are not on the pasted object
		boolean checkAgain = true;
		while (checkAgain)
		{
			checkAgain = false;
			for(Iterator it = destNode.getVariables(); it.hasNext(); )
			{
				Variable destvar = (Variable)it.next();
				Variable.Key key = destvar.getKey();
				Variable srcVar = srcNode.getVar(key.getName());
				if (srcVar != null) continue;
				srcNode.delVar(key);
				checkAgain = true;
				break;
			}
		}

		// make sure all variables are on the node
		destNode.copyVars(srcNode);

		// copy any special user bits
		destNode.lowLevelSetUserbits(srcNode.lowLevelGetUserbits());
		destNode.clearExpanded();
		destNode.clearShortened();
		destNode.clearWiped();
		destNode.clearLocked();

		return(destNode);
	}

	/**
	 * Method to paste one arc onto another.
	 * @param destArc the destination arc that will be replaced.
	 * @param srcArc the source arc that will replace it.
	 * @return the replaced arc (null on error).
	 */
	private static ArcInst pasteArcToArc(ArcInst destArc, ArcInst srcArc)
	{
		// make sure they have the same type
		if (destArc.getProto() != srcArc.getProto())
		{
			destArc = destArc.replace(srcArc.getProto());
			if (destArc == null) return null;
		}

		// make the widths the same
		double dw = srcArc.getWidth() - destArc.getWidth();
		if (dw != 0)
			destArc.modify(dw, 0, 0, 0, 0);

		// remove variables that are not on the pasted object
		boolean checkAgain = true;
		while (checkAgain)
		{
			checkAgain = false;
			for(Iterator it = destArc.getVariables(); it.hasNext(); )
			{
				Variable destvar = (Variable)it.next();
				Variable.Key key = destvar.getKey();
				Variable srcVar = srcArc.getVar(key.getName());
				if (srcVar != null) continue;
				destArc.delVar(key);
				checkAgain = true;
				break;
			}
		}

		// make sure all variables are on the arc
		for(Iterator it = srcArc.getVariables(); it.hasNext(); )
		{
			Variable srcvar = (Variable)it.next();
			Variable.Key key = srcvar.getKey();
			Variable destVar = destArc.newVar(key, srcvar.getObject());
			if (destVar != null)
				destVar.setDescriptor(srcvar.getTextDescriptor());
		}

		// make sure the constraints and other userbits are the same
		if (srcArc.isRigid()) destArc.setRigid(); else destArc.clearRigid();
		if (srcArc.isFixedAngle()) destArc.setFixedAngle(); else destArc.clearFixedAngle();
		if (srcArc.isSlidable()) destArc.setSlidable(); else destArc.clearSlidable();
		if (srcArc.isExtended()) destArc.setExtended(); else destArc.clearExtended();
		if (srcArc.isNegated()) destArc.setNegated(); else destArc.clearNegated();
		if (srcArc.isDirectional()) destArc.setDirectional(); else destArc.clearDirectional();
		if (srcArc.isSkipHead()) destArc.setSkipHead(); else destArc.clearSkipHead();
		if (srcArc.isSkipTail()) destArc.setSkipTail(); else destArc.clearSkipTail();
		if (srcArc.isReverseEnds()) destArc.setReverseEnds(); else destArc.clearReverseEnds();
		if (srcArc.isHardSelect()) destArc.setHardSelect(); else destArc.clearHardSelect();
		return destArc;
	}

}
