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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;

import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.EventListener;

/*
 * Class for managing the circuitry clipboard (for copy and paste).
 */
public class Clipboard
{
	/** The only Clipboard object. */					private static Clipboard theClipboard = new Clipboard();
	/** The Clipboard Library. */						private static Library clipLib = null;
	/** The Clipboard Cell. */							private static Cell clipCell;

	private static boolean dupDistSet = false;
	private static double dupX, dupY;

	/**
	 * The constructor gets called only once.
	 * It creates the clipboard Library and Cell.
	 */
	private Clipboard()
	{
	}

	private static void init()
	{
		if (clipLib == null)
		{
			clipLib = Library.newInstance("Clipboard!!", null);
			clipLib.setHidden();
		}
		if (clipCell == null)
		{
			clipCell = Cell.newInstance(clipLib, "Clipboard!!");
		}
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

	private static class CopyObjects extends Job
	{
		protected CopyObjects()
		{
			super("Copy", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

		public boolean doIt()
		{
			// get objects to copy
			List geoms = Highlight.getHighlighted(true, true);
			if (geoms.size() == 0)
			{
				System.out.println("First select objects to copy");
				return false;
			}

			// determine the cell with these geometrics
			EditWindow wnd = EditWindow.needCurrent();
			if (wnd == null) return false;
			Cell parent = wnd.getCell();

			// remove contents of clipboard
			clear();

            // get offset of highlighted objects from mouse
            Point2D mouse = ClickZoomWireListener.theOne.getLastMouse();
            Point2D mouseDB = wnd.screenToDatabase((int)mouse.getX(), (int)mouse.getY());
            EditWindow.gridAlign(mouseDB);

			// copy objects to clipboard
			copyListToCell(wnd, geoms, parent, clipCell, false, mouseDB.getX(), mouseDB.getY());
			return true;
		}
	}

	public static void cut()
	{
        CutObjects job = new CutObjects();
	}

	private static class CutObjects extends Job
	{
		protected CutObjects()
		{
			super("Cut", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			startJob();
		}

		public boolean doIt()
		{
			// get objects to cut
			List geoms = Highlight.getHighlighted(true, true);
			if (geoms.size() == 0)
			{
				System.out.println("First select objects to cut");
				return false;
			}

			// determine the cell with these geometrics
			EditWindow wnd = EditWindow.needCurrent();
			if (wnd == null) return false;
			Cell parent = wnd.getCell();

			// remove contents of clipboard
			clear();

            // get offset of highlighted objects from mouse
            Point2D mouse = ClickZoomWireListener.theOne.getLastMouse();
            Point2D mouseDB = wnd.screenToDatabase((int)mouse.getX(), (int)mouse.getY());
            EditWindow.gridAlign(mouseDB);

			// make sure deletion is allowed
			if (CircuitChanges.cantEdit(parent, null, true)) return false;

			// copy objects to clipboard
			copyListToCell(wnd, geoms, parent, clipCell, false, mouseDB.getX(), mouseDB.getY());

			// and delete the original objects
			CircuitChanges.eraseObjectsInList(parent, geoms);
			return true;
		}
	}

    public static void duplicate()
    {
        DuplicateObjects job = new DuplicateObjects();
    }

	private static class DuplicateObjects extends Job
    {
        protected DuplicateObjects()
        {
            super("Duplicate", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt()
        {
            // get objects to copy
            List geoms = Highlight.getHighlighted(true, true);
            if (geoms.size() == 0)
            {
                System.out.println("First select objects to copy");
                return false;
            }

            // determine the cell with these geometrics
            EditWindow wnd = EditWindow.needCurrent();
            if (wnd == null) return false;
            Cell parent = wnd.getCell();

            // remove contents of clipboard
            clear();

            // get offset of highlighted objects from mouse
            Point2D mouse = ClickZoomWireListener.theOne.getLastMouse();
            Point2D mouseDB = wnd.screenToDatabase((int)mouse.getX(), (int)mouse.getY());
            EditWindow.gridAlign(mouseDB);

            // copy objects to clipboard
            copyListToCell(wnd, geoms, parent, clipCell, false, mouseDB.getX(), mouseDB.getY());

            Highlight.clear();

            paste();
			return true;
        }
    }

	public static void paste()
	{
		// get objects to paste
		Clipboard.init();
		int nTotal = clipCell.getNumNodes();
		int aTotal = clipCell.getNumArcs();
		int total = nTotal + aTotal;
		if (total == 0)
		{
			System.out.println("Nothing in the clipboard to paste");
			return;
		}

		// find out where the paste is going
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;

		// special case of pasting on top of selected objects
		List geoms = Highlight.getHighlighted(true, true);
		if (geoms.size() > 0)
		{
			// can only paste a single object onto selection
			if (nTotal == 2 && aTotal == 1)
			{
				ArcInst ai = (ArcInst)clipCell.getArcs().next();
				NodeInst niHead = ai.getHead().getPortInst().getNodeInst();
				NodeInst niTail = ai.getTail().getPortInst().getNodeInst();
				Iterator nIt = clipCell.getNodes();
				NodeInst ni1 = (NodeInst)nIt.next();
				NodeInst ni2 = (NodeInst)nIt.next();
				if ((ni1 == niHead && ni2 == niTail) ||
					(ni1 == niTail && ni2 == niHead)) nTotal = 0;
				total = nTotal + aTotal;
			}
			if (total > 1)
			{
				System.out.println("Can only paste a single object on top of selected objects");
				return;
			}
			Highlight.clear();
			Highlight.finished();
			for(Iterator it = geoms.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (geom instanceof NodeInst && nTotal == 1)
				{
					NodeInst ni = (NodeInst)geom;
					PasteNodeToNode job = new PasteNodeToNode(ni, (NodeInst)clipCell.getNodes().next());
				} else if (geom instanceof ArcInst && aTotal == 1)
				{
					ArcInst ai = (ArcInst)geom;
					PasteArcToArc job = new PasteArcToArc(ai, (ArcInst)clipCell.getArcs().next());
				}
			}
			return;
		}

		// make list of things to paste
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

		if (!dupDistSet)
		{
			dupDistSet = true;
            dupX = dupY = 2;
		}
        if (User.isMoveAfterDuplicate())
		{
			EventListener currentListener = WindowFrame.getListener();
			WindowFrame.setListener(new PasteListener(wnd, pasteList, currentListener, null));
		} else
		{
            // get offset of highlighted objects from mouse
            Point2D mouse = ClickZoomWireListener.theOne.getLastMouse();
            Point2D mouseDB = wnd.screenToDatabase((int)mouse.getX(), (int)mouse.getY());
            EditWindow.gridAlign(mouseDB);
		    PasteObjects job = new PasteObjects(pasteList, -dupX - mouseDB.getX(), -dupY - mouseDB.getY());
		}
	}

	private static class PasteArcToArc extends Job
	{
		ArcInst src, dst;

		protected PasteArcToArc(ArcInst dst, ArcInst src)
		{
			super("Paste Arc to Arc", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.src = src;
			this.dst = dst;
			startJob();
		}

		public boolean doIt()
		{
			// make sure pasting is allowed
			if (CircuitChanges.cantEdit(dst.getParent(), null, true)) return false;

			ArcInst ai = pasteArcToArc(dst, src);
			if (ai == null) System.out.println("Nothing was pasted");
			return true;
		}
	}

	private static class PasteNodeToNode extends Job
	{
		NodeInst src, dst;

		protected PasteNodeToNode(NodeInst src, NodeInst dst)
		{
			super("Paste Node to Node", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.src = src;
			this.dst = dst;
			startJob();
		}

		public boolean doIt()
		{
			// make sure pasting is allowed
			if (CircuitChanges.cantEdit(dst.getParent(), null, true)) return false;

			NodeInst ni = pasteNodeToNode(dst, src);
			if (ni == null) System.out.println("Nothing was pasted");
			return true;
		}
	}

	private static class PasteObjects extends Job
	{
		List pasteList;
		double dX, dY;

		protected PasteObjects(List pasteList, double dX, double dY)
		{
			super("Paste", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.pasteList = pasteList;
			this.dX = dX;
			this.dY = dY;
			startJob();
		}

		public boolean doIt()
		{
			// find out where the paste is going
			EditWindow wnd = EditWindow.needCurrent();
			Cell parent = wnd.getCell();

			// make sure pasting is allowed
			if (CircuitChanges.cantEdit(parent, null, true)) return false;

			// paste them into the current cell
			copyListToCell(wnd, pasteList, clipCell, parent, true, dX, dY);
			return true;
		}
	}

	/**
	 * Returns a printable version of this Clipboard.
	 * @return a printable version of this Clipboard.
	 */
	public String toString() { return "Clipboard"; }

	private static class NodeNameCaseInsensitive implements Comparator
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

	private static class ArcNameCaseInsensitive implements Comparator
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

	private static class ExportNameCaseInsensitive implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Export e1 = (Export)o1;
			Export e2 = (Export)o2;
			String s1 = e1.getName();
			String s2 = e1.getName();
			if (s1 == null) s1 = "";
			if (s2 == null) s2 = "";
			return s1.compareToIgnoreCase(s2);
		}
	}

	/**
	 * Method to copy the list of objects in "list" (NOGEOM terminated) from "fromCell"
	 * to "toCell".  If "highlight" is true, highlight the objects in the new cell.
     * mouseX and mouseY are the coordinates of the mouse if copying, or the negative
     * coordinates of the mouse if pasting.
	 */
	private static void copyListToCell(EditWindow wnd, List list, Cell fromCell, Cell toCell, boolean highlight,
		double mouseX, double mouseY)
	{
        double dX = -mouseX;
        double dY = -mouseY;

		// make sure they are all in the same cell
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (fromCell != geom.getParent())
			{
				System.out.println("All duplicated objects must be in the same cell");
				return;
			}
		}

		// set the technology of the new cell from the old cell if the new is empty
//		if (toCell->firstnodeinst == NONODEINST ||
//			(toCell->firstnodeinst->proto == gen_cellcenterprim &&
//				toCell->firstnodeinst->nextnodeinst == NONODEINST))
//		{
//			toCell->tech = fromCell->tech;
//		}

		// mark all nodes (including those touched by highlighted arcs)
		FlagSet inAreaFlag = Geometric.getFlagSet(1);
		for(Iterator it = fromCell.getNodes(); it.hasNext(); )
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
			ni.setBit(inAreaFlag);
		}

		// build a list that includes all nodes touching copied arcs
		List theNodes = new ArrayList();
		for(Iterator it = fromCell.getNodes(); it.hasNext(); )
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
			Cell niCell = (Cell)ni.getProto();
			if (niCell.isAChildOf(toCell))
			{
				System.out.println("Cannot: that would be recursive (cell " +
					toCell.describe() + " is beneath cell " + ni.getProto().describe() + ")");
				return;
			}
		}

		// figure out lower-left corner of this collection of objects
		Iterator nit = theNodes.iterator();
		NodeInst niFirst = (NodeInst)nit.next();
		Point2D corner = new Point2D.Double();
		corner.setLocation(niFirst.getAnchorCenter());
		for(; nit.hasNext(); )
		{
			NodeInst ni = (NodeInst)nit.next();
			Point2D pt = ni.getAnchorCenter();
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

		// initialize for queueing creation of new exports
		List queuedExports = new ArrayList();

		// sort the nodes by name
		Collections.sort(theNodes, new NodeNameCaseInsensitive());

		// create the new objects
		for(Iterator it = theNodes.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() == Generic.tech.cellCenterNode && toCell.alreadyCellCenter()) continue;
			double width = ni.getXSize();
			if (ni.isXMirrored()) width = -width;
			double height = ni.getYSize();
			if (ni.isYMirrored()) height = -height;
			String name = null;
			if (ni.isUsernamed())
				name = ElectricObject.uniqueObjectName(ni.getName(), toCell, NodeInst.class);
			NodeInst newNi = NodeInst.newInstance(ni.getProto(),
				new Point2D.Double(ni.getAnchorCenterX()+dX, ni.getAnchorCenterY()+dY),
					width, height, ni.getAngle(), toCell, name);
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
					name = ElectricObject.uniqueObjectName(ai.getName(), toCell, ArcInst.class);
				ArcInst newAr = ArcInst.newInstance(ai.getProto(), ai.getWidth(),
					headPi, new Point2D.Double(ai.getHead().getLocation().getX() + dX, ai.getHead().getLocation().getY() + dY),
					tailPi, new Point2D.Double(ai.getTail().getLocation().getX() + dX, ai.getTail().getLocation().getY() + dY), name);
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
				if (ni == null) continue;

				// special case for displayable text on invisible pins
				if (ni.isInvisiblePinWithText())
				{
					Poly [] polys = ni.getAllText(false, wnd);
					if (polys == null) continue;
					for(int i=0; i<polys.length; i++)
					{
						Poly poly = polys[i];
						Highlight h = Highlight.addText(ni, toCell, poly.getVariable(), poly.getName());
					}
					continue;
				}
				Highlight h = Highlight.addElectricObject(ni, toCell);
			}
			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (geom instanceof NodeInst) continue;
				ArcInst ai = (ArcInst)geom;
				ai = (ArcInst)ai.getTempObj();
				Highlight h = Highlight.addElectricObject(ai, toCell);
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
	 * The port is being copied from an original port "origPp".  Returns true on error.
	 */
	public static void createQueuedExports(List queuedExports)
	{
		/* sort the ports by their original name */
		Collections.sort(queuedExports, new ExportNameCaseInsensitive());

		for(Iterator it = queuedExports.iterator(); it.hasNext(); )
		{
			Export origPp = (Export)it.next();
			PortInst pi = origPp.getOriginalPort();
			NodeInst ni = pi.getNodeInst();
			ni = (NodeInst)ni.getTempObj();

			PortProto pp = pi.getPortProto();
			PortInst newPi = ni.findPortInstFromProto(pp);

			Cell cell = ni.getParent();
			String portName = ElectricObject.uniqueObjectName(origPp.getName(), cell, PortProto.class);
			Export newPp = Export.newInstance(cell, newPi, portName);
			if (newPp == null) return;
			newPp.setTextDescriptor(origPp.getTextDescriptor());
			newPp.copyVars(origPp);
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
			double dX = srcNode.getXSize() - destNode.getXSize();
			double dY = srcNode.getYSize() - destNode.getYSize();
			if (dX != 0 || dY != 0)
			{
				double dlx = -dX/2;   double dhx = dX/2;
				double dly = -dY/2;   double dhy = dY/2;
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
				Variable destVar = (Variable)it.next();
				Variable.Key key = destVar.getKey();
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
				Variable destVar = (Variable)it.next();
				Variable.Key key = destVar.getKey();
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
			Variable srcVar = (Variable)it.next();
			Variable.Key key = srcVar.getKey();
			Variable destVar = destArc.newVar(key, srcVar.getObject());
			if (destVar != null)
				destVar.setTextDescriptor(srcVar.getTextDescriptor());
		}

		// make sure the constraints and other userbits are the same
		if (srcArc.isRigid()) destArc.setRigid(); else destArc.clearRigid();
		if (srcArc.isFixedAngle()) destArc.setFixedAngle(); else destArc.clearFixedAngle();
		if (srcArc.isSlidable()) destArc.setSlidable(); else destArc.clearSlidable();
		if (srcArc.isExtended()) destArc.setExtended(); else destArc.clearExtended();
//		if (srcArc.isNegated()) destArc.setNegated(); else destArc.clearNegated();
		if (srcArc.isDirectional()) destArc.setDirectional(); else destArc.clearDirectional();
		if (srcArc.isSkipHead()) destArc.setSkipHead(); else destArc.clearSkipHead();
		if (srcArc.isSkipTail()) destArc.setSkipTail(); else destArc.clearSkipTail();
		if (srcArc.isReverseEnds()) destArc.setReverseEnds(); else destArc.clearReverseEnds();
		if (srcArc.isHardSelect()) destArc.setHardSelect(); else destArc.clearHardSelect();
		return destArc;
	}

	/**
	 * Class to handle the interactive drag after a paste.
	 */
	private static class PasteListener
		implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
	{
		private EditWindow wnd;
		private List pasteList;
		private EventListener currentListener;
		private int origX, origY;                   // database units
		private double oX, oY;                      // database units

        /**
         * Create a new paste listener
         * @param wnd Controlling window
         * @param pasteList list of objects to paste
         * @param currentListener listener to restore when done
         * @param startPaste starting location of paste, in database units. If null,
         * uses current mouse location.
         */
		public PasteListener(EditWindow wnd, List pasteList, EventListener currentListener, Point2D startPaste)
		{
			this.wnd = wnd;
			this.pasteList = pasteList;
			this.currentListener = currentListener;

			// determine the initial offset of the objects if not given
            Point2D mouseDB;
            if (startPaste == null) {
			    // get starting point from current mouse location
                Point2D mouse = ClickZoomWireListener.theOne.getLastMouse();
                mouseDB = wnd.screenToDatabase((int)mouse.getX(), (int)mouse.getY());
            } else {
                // get starting point from given coords
                mouseDB = startPaste;
            }
            EditWindow.gridAlign(mouseDB);
            origX = (int)mouseDB.getX(); origY = (int)mouseDB.getY();
            oX = mouseDB.getX();
            oY = mouseDB.getY();

			showList();
		}

		private void showList()
		{
			Cell cell = wnd.getCell();
			Highlight.clear();
			for(Iterator it = pasteList.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				Point2D [] points = null;
				if (geom instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)geom;
					Poly poly = ai.makePoly(ai.getLength(), ai.getWidth() - ai.getProto().getWidthOffset(), Poly.Type.CLOSED);
					points = poly.getPoints();
				} else
				{
					NodeInst ni = (NodeInst)geom;
					if (ni.isInvisiblePinWithText())
					{
						// find text on the invisible pin
						for(Iterator vIt = ni.getVariables(); vIt.hasNext(); )
						{
							Variable var = (Variable)vIt.next();
							if (var.isDisplay())
							{
								points = Highlight.describeHighlightText(wnd, geom, var, null);
								break;
							}
						}
					}
					if (points != null)
					{
						for(int i=0; i<points.length; i += 2)
						{
							double fX = points[i].getX();
							double fY = points[i].getY();
							double tX = points[i+1].getX();
							double tY = points[i+1].getY();
							Highlight.addLine(new Point2D.Double(fX+oX, fY+oY), new Point2D.Double(tX+oX, tY+oY), cell);
						}
						continue;
					}
					SizeOffset so = Technology.getSizeOffset(ni);
					AffineTransform trans = ni.rotateOutAboutTrueCenter();
					double nodeLowX = ni.getTrueCenterX() - ni.getXSize()/2 + so.getLowXOffset();
					double nodeHighX = ni.getTrueCenterX() + ni.getXSize()/2 - so.getHighXOffset();
					double nodeLowY = ni.getTrueCenterY() - ni.getYSize()/2 + so.getLowYOffset();
					double nodeHighY = ni.getTrueCenterY() + ni.getYSize()/2 - so.getHighYOffset();
					double nodeX = (nodeLowX + nodeHighX) / 2;
					double nodeY = (nodeLowY + nodeHighY) / 2;
					Poly poly = new Poly(nodeX, nodeY, nodeHighX-nodeLowX, nodeHighY-nodeLowY);
					poly.transform(trans);
					points = poly.getPoints();
				}
				if (points != null)
				{
					for(int i=0; i<points.length; i++)
					{
						int lastI = i - 1;
						if (lastI < 0) lastI = points.length - 1;
						double fX = points[lastI].getX();
						double fY = points[lastI].getY();
						double tX = points[i].getX();
						double tY = points[i].getY();
						Highlight.addLine(new Point2D.Double(fX+oX, fY+oY), new Point2D.Double(tX+oX, tY+oY), cell);
					}
				}
			}
			Highlight.finished();
		}

		public void mousePressed(MouseEvent evt)
		{
		}

		public void mouseDragged(MouseEvent evt)
		{
            mouseMoved(evt);
		}

		public void mouseReleased(MouseEvent evt)
		{
            boolean ctrl = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;
            Point2D mouseDB = wnd.screenToDatabase((int)evt.getX(), (int)evt.getY());

            // if user holds control, only move orthogonally
            if (ctrl) {
                mouseDB = ClickZoomWireListener.convertToOrthogonal(new Point2D.Double(origX, origY), mouseDB);
            }
            EditWindow.gridAlign(mouseDB);
            oX = mouseDB.getX();
            oY = mouseDB.getY();
            showList();

            WindowFrame.setListener(currentListener);
            PasteObjects job = new PasteObjects(pasteList, -oX, -oY);
		}

		public void mouseMoved(MouseEvent evt)
        {
            boolean ctrl = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;
            Point2D mouseDB = wnd.screenToDatabase((int)evt.getX(), (int)evt.getY());

            // if user holds control, only move orthogonally
            if (ctrl) {
                mouseDB = ClickZoomWireListener.convertToOrthogonal(new Point2D.Double(origX, origY), mouseDB);
            }
            EditWindow.gridAlign(mouseDB);
            oX = mouseDB.getX();
            oY = mouseDB.getY();
            showList();
            wnd.repaint();
        }

		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}
		public void mouseWheelMoved(MouseWheelEvent e) {}
		public void keyPressed(KeyEvent evt) {
            int chr = evt.getKeyCode();
            if (chr == KeyEvent.VK_ESCAPE) {
                // abort on ESC
                Highlight.clear();
                Highlight.finished();
                WindowFrame.setListener(currentListener);
                wnd.repaint();
            }
        }
		public void keyReleased(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}
	}

}
