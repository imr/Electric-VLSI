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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.ui.ClickZoomWireListener;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

/*
 * Class for managing the circuitry clipboard (for copy and paste).
 */
public class Clipboard
{
	/** The only Clipboard object. */					private static Clipboard theClipboard = new Clipboard();
	/** The Clipboard Library. */						private static Library   clipLib = null;
	/** The Clipboard Cell. */							private static Cell      clipCell;
	/** the last node that was duplicated */			private static NodeInst  lastDup = null;
	/** the amount that the last node moved */			private static double    lastDupX = 10, lastDupY = 10;

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

	// this is really only for debugging
    public static void editClipboard() {
        EditWindow wnd = EditWindow.getCurrent();
        wnd.setCell(clipCell, VarContext.globalContext);
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

        // Delete all variables
        List varsToDelete = new ArrayList();
        for(Iterator it = clipCell.getVariables(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
            clipCell.delVar(var.getKey());
			//varsToDelete.add(var);
		}
	}

	public static void copy()
	{
		// special case: if one text object is selected, copy its text to the system clipboard
		copySelectedText();

		CopyObjects job = new CopyObjects(MenuCommands.getHighlighted());
	}

	/**
	 * Method to copy any selected text to the system-wide clipboard.
	 */
	private static void copySelectedText()
	{
		List highlights = MenuCommands.getHighlighted();
		if (highlights.size() == 1)
		{
			Highlight h = (Highlight)highlights.get(0);
			if (h.getType() == Highlight.Type.TEXT)
			{
				String selected = null;
				Variable var = h.getVar();
				ElectricObject eObj = h.getElectricObject();
				if (var != null)
				{
					selected = var.describe(-1);
				} else if (h.getName() != null)
				{
					selected = h.getName().toString();
				} else if (eObj instanceof Export)
				{
					selected = ((Export)eObj).getName();
				} else if (eObj instanceof NodeInst)
				{
					selected = ((NodeInst)eObj).getProto().describe();
				}
				if (selected != null)
				{
					// put the text in the clipboard
					java.awt.datatransfer.Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
					Transferable transferable = new StringSelection(selected);
					cb.setContents(transferable, null);
				}
			}
		}
	}

	private static class CopyObjects extends Job
	{
        private List highlights;

		protected CopyObjects(List highlights)
		{
			super("Copy", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.highlights = highlights;
			startJob();
		}

		public boolean doIt()
		{
			// get highlights to copy
			if (highlights.size() == 0)
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

			// copy objects to clipboard
			copyListToCell(null, highlights, parent, clipCell, new Point2D.Double(0,0),
				User.isDupCopiesExports(), User.isArcsAutoIncremented());
			return true;
		}
	}

	public static void cut()
	{
		// special case: if one text object is selected, copy its text to the system clipboard
		copySelectedText();

		CutObjects job = new CutObjects(MenuCommands.getHighlighted());
	}

	private static class CutObjects extends Job
	{
        private List highlights;

		protected CutObjects(List highlights)
		{
			super("Cut", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.highlights = highlights;
			startJob();
		}

		public boolean doIt()
		{
			// get objects to cut
			if (highlights.size() == 0)
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

			// make sure deletion is allowed
			if (CircuitChanges.cantEdit(parent, null, true) != 0) return false;
			List deleteList = new ArrayList();
			for(Iterator it = highlights.iterator(); it.hasNext(); )
			{
				Highlight h = (Highlight)it.next();
				if (h.getType() == Highlight.Type.EOBJ)
				{
					ElectricObject eObj = h.getElectricObject();
					if (eObj instanceof PortInst) eObj = ((PortInst)eObj).getNodeInst();
					if (eObj instanceof NodeInst)
					{
						int errorCode = CircuitChanges.cantEdit(parent, (NodeInst)eObj, true);
						if (errorCode < 0) return false;
						if (errorCode > 0) continue;
					}
				}
				deleteList.add(h);
			}
			highlights = deleteList;

			// copy objects to clipboard
			copyListToCell(null, highlights, parent, clipCell, new Point2D.Double(0, 0),
				User.isDupCopiesExports(), User.isArcsAutoIncremented());

			// and delete the original objects
			CircuitChanges.eraseObjectsInList(parent, highlights);
			return true;
		}
	}

    public static void duplicate()
    {
        DuplicateObjects job = new DuplicateObjects(MenuCommands.getHighlighted());
    }

	/**
	 * Method to track movement of the object that was just duplicated.
	 * By following subsequent changes to that node, future duplications know where to place their copies.
	 * @param ni the NodeInst that has just moved.
	 * @param lastX the previous center X of the NodeInst.
	 * @param lastY the previous center Y of the NodeInst.
	 */
	public static void nodeMoved(NodeInst ni, double lastX, double lastY)
	{
		if (ni != lastDup) return;
		lastDupX += ni.getAnchorCenterX() - lastX;
		lastDupY += ni.getAnchorCenterY() - lastY;
	}

	private static class DuplicateObjects extends Job
    {
        private List highlights;

        protected DuplicateObjects(List highlights)
        {
            super("Duplicate", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.highlights = highlights;
            startJob();
        }

        public boolean doIt()
        {
            // get objects to copy
            if (highlights.size() == 0)
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

            // copy objects to clipboard
            copyListToCell(null, highlights, parent, clipCell, new Point2D.Double(0, 0),
            	User.isDupCopiesExports(), User.isArcsAutoIncremented());

            Highlighter highlighter = wnd.getHighlighter();
            if (highlighter != null) highlighter.clear();

            paste(true);
			return true;
        }
    }

	public static void paste(boolean duplicate)
	{
		// get objects to paste
		Clipboard.init();
		int nTotal = clipCell.getNumNodes();
		int aTotal = clipCell.getNumArcs();
        int vTotal = clipCell.getNumVariables();
		int total = nTotal + aTotal + vTotal;
		if (total == 0)
		{
			System.out.println("Nothing in the clipboard to paste");
			return;
		}

		// find out where the paste is going
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();

		// special case of pasting on top of selected objects
		List geoms = highlighter.getHighlightedEObjs(true, true);
		if (geoms.size() > 0)
		{
			// can only paste a single object onto selection
			if (nTotal == 2 && aTotal == 1)
			{
				ArcInst ai = (ArcInst)clipCell.getArcs().next();
				NodeInst niHead = ai.getHeadPortInst().getNodeInst();
				NodeInst niTail = ai.getTailPortInst().getNodeInst();
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
			for(Iterator it = geoms.iterator(); it.hasNext(); )
			{
				Geometric geom = (Geometric)it.next();
				if (geom instanceof NodeInst && nTotal == 1)
				{
					NodeInst ni = (NodeInst)geom;
					PasteNodeToNode job = new PasteNodeToNode(ni, (NodeInst)clipCell.getNodes().next(), highlighter);
				} else if (geom instanceof ArcInst && aTotal == 1)
				{
					ArcInst ai = (ArcInst)geom;
					PasteArcToArc job = new PasteArcToArc(ai, (ArcInst)clipCell.getArcs().next(), highlighter);
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
        for (Iterator it = clipCell.getVariables(); it.hasNext(); )
        {
            Variable var = (Variable)it.next();
            pasteList.add(var);
        }

        if (pasteList.size() == 0) return;

        if (!duplicate || User.isMoveAfterDuplicate())
		{
			EventListener currentListener = WindowFrame.getListener();
			WindowFrame.setListener(new PasteListener(wnd, pasteList, currentListener));
		} else
		{
			Point2D refPastePoint = new Point2D.Double(lastDupX, lastDupY);
		    PasteObjects job = new PasteObjects(pasteList, refPastePoint.getX(), refPastePoint.getY());
		}
	}

	private static class PasteArcToArc extends Job
	{
		ArcInst src, dst;
        Highlighter highlighter;

		protected PasteArcToArc(ArcInst dst, ArcInst src, Highlighter highlighter)
		{
			super("Paste Arc to Arc", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.src = src;
			this.dst = dst;
            this.highlighter = highlighter;
			startJob();
		}

		public boolean doIt()
		{
			// make sure pasting is allowed
			if (CircuitChanges.cantEdit(dst.getParent(), null, true) != 0) return false;

			ArcInst ai = pasteArcToArc(dst, src);
			if (ai == null) System.out.println("Nothing was pasted");
            if (ai != null) {
                highlighter.clear();
                highlighter.addElectricObject(ai, ai.getParent());
                highlighter.finished();
            }
			return true;
		}
	}

	private static class PasteNodeToNode extends Job
	{
		NodeInst src, dst;
        Highlighter highlighter;

		protected PasteNodeToNode(NodeInst dst, NodeInst src, Highlighter highlighter)
		{
			super("Paste Node to Node", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.src = src;
			this.dst = dst;
            this.highlighter = highlighter;
			startJob();
		}

		public boolean doIt()
		{
			// make sure pasting is allowed
			if (CircuitChanges.cantEdit(dst.getParent(), null, true) != 0) return false;

			NodeInst ni = pasteNodeToNode(dst, src);
			if (ni == null) System.out.println("Nothing was pasted");
            if (ni != null) {
                highlighter.clear();
                highlighter.addElectricObject(ni, ni.getParent());
                highlighter.finished();
            }
			return true;
		}
	}

	private static class PasteObjects extends Job
	{
		List pasteList;
		double dX, dY;

		protected PasteObjects(List pasteList, double dX, double dY)
		{
			super("Paste", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
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
			if (CircuitChanges.cantEdit(parent, null, true) != 0) return false;

			// paste them into the current cell
			copyListToCell(wnd, pasteList, clipCell, parent, new Point2D.Double(dX, dY),
				User.isDupCopiesExports(), User.isArcsAutoIncremented());

			// also copy any variables on the clipboard cell
			for(Iterator it = clipCell.getVariables(); it.hasNext(); )
			{
				Variable var = (Variable)it.next();
				if (!var.isDisplay()) continue;
				Variable cellVar = parent.newVar(var.getKey(), var.getObject());
				if (cellVar != null)
				{
					cellVar.setTextDescriptor(var.getTextDescriptor());
					cellVar.setOff(cellVar.getXOff() + dX, cellVar.getYOff() + dY);
//					cellVar.setCode(var.getCode());
//					cellVar.setDisplay(true);
				}
			}
			return true;
		}
	}

	/**
	 * Returns a printable version of this Clipboard.
	 * @return a printable version of this Clipboard.
	 */
	public String toString() { return "Clipboard"; }

	/**
	 * Method to copy the list of Geometrics to a new Cell.
	 * @param wnd the EditWindow in which this is happening (if null, do not highlight copied Geometrics).
	 * @param list the list of Geometrics to copy.
	 * @param fromCell the source cell of the Geometrics.
	 * @param toCell the destination cell of the Geometrics.
	 * @param delta an offset for all of the copied Geometrics.
	 * @param copyExports true to copy exports.
	 * @param uniqueArcs true to generate unique arc names.
	 */
	public static void copyListToCell(EditWindow wnd, List list, Cell fromCell, Cell toCell,
		Point2D delta, boolean copyExports, boolean uniqueArcs)
	{
		// make sure they are all in the same cell
		for(Iterator it = list.iterator(); it.hasNext(); )
		{
			Object obj = it.next();
			if (obj instanceof Highlight) obj = ((Highlight)obj).getGeometric();
			if (!(obj instanceof Geometric)) continue;
			Geometric geom = (Geometric)obj;

			if (fromCell != geom.getParent())
			{
				System.out.println("All duplicated objects must be in the same cell");
				return;
			}
		}

        // make a list of all objects to be copied (includes end points of arcs)
        List theNodes = new ArrayList();
        List theArcs = new ArrayList();
        List theTextVariables = new ArrayList();
        for (Iterator it = list.iterator(); it.hasNext(); )
        {
	        Object obj = it.next();
            Highlight h = null;
			if (obj instanceof Highlight)
            {
                h = (Highlight)obj;
                obj = h.getGeometric();
            }
            if (obj instanceof Geometric)
            {
                Geometric geom = (Geometric)obj;

                if (geom instanceof NodeInst)
                {
                    if (!theNodes.contains(geom)) theNodes.add(geom);
                }
                if (geom instanceof ArcInst)
                {
                    ArcInst ai = (ArcInst)geom;
                    theArcs.add(ai);
                    NodeInst head = ai.getHeadPortInst().getNodeInst();
                    NodeInst tail = ai.getTailPortInst().getNodeInst();
                    if (!theNodes.contains(head)) theNodes.add(head);
                    if (!theNodes.contains(tail)) theNodes.add(tail);
                }
            }
            // For text variables
            if (h != null && h.getType() == Highlight.Type.TEXT)
            {
                Variable var = h.getVar();
                if (var != null && h.getElectricObject() instanceof Cell)
                    theTextVariables.add(var);
            }
        }

		if (theNodes.size() == 0 && theTextVariables.size() == 0) return;

		// check for recursion
		for(Iterator it = theNodes.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() instanceof PrimitiveNode) continue;
			Cell niCell = (Cell)ni.getProto();
            if (Cell.isInstantiationRecursive(niCell, toCell))
			{
				System.out.println("Cannot: that would be recursive (cell " +
					toCell.describe() + " is beneath cell " + ni.getProto().describe() + ")");
				return;
			}
		}

        EditWindow.gridAlign(delta);
        double dX = delta.getX();
        double dY = delta.getY();

		// sort the nodes by name
		Collections.sort(theNodes);

		// create the new nodes
		HashMap newNodes = new HashMap();
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
					width, height, toCell, ni.getAngle(), name, ni.getTechSpecific());
			if (newNi == null)
			{
				System.out.println("Cannot create node");
				return;
			}
			newNi.copyStateBits(ni);
			newNi.clearWiped();
			newNi.clearShortened();
			newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_PROTO_TD);
			newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_NAME_TD);
			newNi.copyVarsFrom(ni);
			newNodes.put(ni, newNi);
			lastDup = newNi;

			// copy the ports, too
            List portInstsToExport = new ArrayList();
            HashMap originalExports = new HashMap();
			if (copyExports)
			{
				for(Iterator eit = ni.getExports(); eit.hasNext(); )
				{
					Export pp = (Export)eit.next();
                    PortInst pi = ExportChanges.getNewPortFromReferenceExport(newNi, pp);
                    portInstsToExport.add(pi);
					originalExports.put(pi, pp);
				}
			}
            ExportChanges.reExportPorts(portInstsToExport, true, true, false, originalExports);
		}

		HashMap newArcs = new HashMap();
		if (theArcs.size() > 0)
		{
			// sort the arcs by name
			Collections.sort(theArcs);

			// for associating old names with new names
			HashMap newArcNames = new HashMap();

			// create the new arcs
			for(Iterator it = theArcs.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				PortInst oldHeadPi = ai.getHeadPortInst();
				NodeInst headNi = (NodeInst)newNodes.get(oldHeadPi.getNodeInst());
				PortInst headPi = headNi.findPortInstFromProto(oldHeadPi.getPortProto());

				PortInst oldTailPi = ai.getTailPortInst();
				NodeInst tailNi = (NodeInst)newNodes.get(oldTailPi.getNodeInst());
				PortInst tailPi = tailNi.findPortInstFromProto(oldTailPi.getPortProto());

				String name = null;
				if (ai.isUsernamed())
				{
					name = ai.getName();
					if (uniqueArcs)
					{
						String newName = (String)newArcNames.get(name);
						if (newName == null)
						{
							newName = ElectricObject.uniqueObjectName(name, toCell, ArcInst.class);
							newArcNames.put(name, newName);
						}
						name = newName;
					}
				}
				ArcInst newAr = ArcInst.newInstance(ai.getProto(), ai.getWidth(),
					headPi, tailPi, new Point2D.Double(ai.getHeadLocation().getX() + dX, ai.getHeadLocation().getY() + dY),
				        new Point2D.Double(ai.getTailLocation().getX() + dX, ai.getTailLocation().getY() + dY), name, ai.getAngle());
				if (newAr == null)
				{
					System.out.println("Cannot create arc");
					return;
				}
				newAr.copyPropertiesFrom(ai);
				newArcs.put(ai, newAr);
			}
		}

		// copy variables on cells
        for(Iterator it = theTextVariables.iterator(); it.hasNext(); )
		{
			Variable var = (Variable)it.next();
			Variable cellVar = toCell.newVar(var.getKey(), var.getObject(), var.getTextDescriptor());
//			if (cellVar != null)
//			{
//				cellVar.setDisplay(var.isDisplay());
//				cellVar.setCode(var.getCode());
//				cellVar.setTextDescriptor(var.getTextDescriptor());
//			}
		}

		// highlight the copy
		if (wnd != null)
		{
            Highlighter highlighter = wnd.getHighlighter();
			highlighter.clear();
			for(Iterator it = theNodes.iterator(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				ni = (NodeInst)newNodes.get(ni);
				if (ni == null) continue;

				// special case for displayable text on invisible pins
				if (ni.isInvisiblePinWithText())
				{
					Poly [] polys = ni.getAllText(false, wnd);
					if (polys == null) continue;
					for(int i=0; i<polys.length; i++)
					{
						Poly poly = polys[i];
                        if (poly == null) continue;
						Highlight h = highlighter.addText(ni, toCell, poly.getVariable(), poly.getName());
					}
					continue;
				}
				Highlight h = highlighter.addElectricObject(ni, toCell);
			}
			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				Object obj = it.next();
				if (!(obj instanceof Geometric)) continue; // Temporary fix?
                Geometric geom = (Geometric)obj;
				if (geom instanceof NodeInst) continue;
				ArcInst ai = (ArcInst)geom;
				ai = (ArcInst)newArcs.get(ai);
				Highlight h = highlighter.addElectricObject(ai, toCell);
			}
			highlighter.finished();
		}
	}

    /**
     * Gets a boundary representing the paste bounds of the list of objects.
     * The corners and center point of the bounds can be used as anchors
     * when pasting the objects interactively. This is all done in database units.
     * Note: you will likely want to grid align any points before using them.
     * @param pasteList a list of Geometrics to paste
     * @return a Rectangle2D that is the paste bounds.
     */
    private static Rectangle2D getPasteBounds(List pasteList) {

        Point2D llcorner = null;
        Point2D urcorner = null;

        // figure out lower-left corner and upper-rigth corner of this collection of objects
        for(Iterator it = pasteList.iterator(); it.hasNext(); )
        {
            Object obj = it.next();
            if ((obj instanceof Variable))
            {
                Variable var = (Variable)obj;
                Poly poly = clipCell.computeTextPoly(EditWindow.needCurrent(), var, null);
                Rectangle2D bounds = poly.getBounds2D();

                if (llcorner == null) {
                    llcorner = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
                    urcorner = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
                    continue;
                }
                if (bounds.getMinX() < llcorner.getX()) llcorner.setLocation(bounds.getMinX(), llcorner.getY());
                if (bounds.getMinY() < llcorner.getY()) llcorner.setLocation(llcorner.getX(), bounds.getMinY());
                if (bounds.getMaxX() > urcorner.getX()) urcorner.setLocation(bounds.getMaxX(), urcorner.getY());
                if (bounds.getMaxY() > urcorner.getY()) urcorner.setLocation(urcorner.getX(), bounds.getMaxY());
            }
            else
            {
                Geometric geom = (Geometric)obj;
                if (geom instanceof NodeInst) {
                    NodeInst ni = (NodeInst)geom;
                    Point2D pt = ni.getAnchorCenter();

                    if (llcorner == null) {
                        llcorner = new Point2D.Double(pt.getX(), pt.getY());
                        urcorner = new Point2D.Double(pt.getX(), pt.getY());
                        continue;
                    }
                    if (pt.getX() < llcorner.getX()) llcorner.setLocation(pt.getX(), llcorner.getY());
                    if (pt.getY() < llcorner.getY()) llcorner.setLocation(llcorner.getX(), pt.getY());
                    if (pt.getX() > urcorner.getX()) urcorner.setLocation(pt.getX(), urcorner.getY());
                    if (pt.getY() > urcorner.getY()) urcorner.setLocation(urcorner.getX(), pt.getY());
                }
                if (geom instanceof ArcInst) {
                    ArcInst ai = (ArcInst)geom;
                    double wid = ai.getWidth() - ai.getProto().getWidthOffset();
                    Poly poly = ai.makePoly(ai.getLength(), wid, Poly.Type.FILLED);
                    Rectangle2D bounds = poly.getBounds2D();

                    if (llcorner == null) {
                        llcorner = new Point2D.Double(bounds.getMinX(), bounds.getMinY());
                        urcorner = new Point2D.Double(bounds.getMaxX(), bounds.getMaxY());
                        continue;
                    }
                    if (bounds.getMinX() < llcorner.getX()) llcorner.setLocation(bounds.getMinX(), llcorner.getY());
                    if (bounds.getMinY() < llcorner.getY()) llcorner.setLocation(llcorner.getX(), bounds.getMinY());
                    if (bounds.getMaxX() > urcorner.getX()) urcorner.setLocation(bounds.getMaxX(), urcorner.getY());
                    if (bounds.getMaxY() > urcorner.getY()) urcorner.setLocation(urcorner.getX(), bounds.getMaxY());
                }
            }
        }

        // figure bounds
        double width = urcorner.getX() - llcorner.getX();
        double height = urcorner.getY() - llcorner.getY();
        Rectangle2D bounds = new Rectangle2D.Double(llcorner.getX(), llcorner.getY(), width, height);
        return bounds;
    }

	/**
	 * Method to "paste" node "srcnode" onto node "destnode", making them the same.
	 * Returns the address of the destination node (null on error).
	 */
	private static NodeInst pasteNodeToNode(NodeInst destNode, NodeInst srcNode)
	{
		destNode = CircuitChanges.replaceNodeInst(destNode, srcNode.getProto(), true, false);
        if (destNode == null) return null;

        destNode.clearExpanded();
        if (srcNode.isExpanded()) destNode.setExpanded();

        if ((destNode.getProto() instanceof PrimitiveNode) && (srcNode.getProto() instanceof PrimitiveNode)) {
            if (srcNode.getProto().getTechnology() == destNode.getProto().getTechnology()) {
                Technology tech = srcNode.getProto().getTechnology();
                tech.setPrimitiveFunction(destNode, srcNode.getFunction());
            }
        }

		// make the sizes the same if they are primitives
		if (destNode.getProto() instanceof PrimitiveNode)
		{
			double dX = srcNode.getXSize() - destNode.getXSize();
			double dY = srcNode.getYSize() - destNode.getYSize();
			if (dX != 0 || dY != 0)
			{
				destNode.modifyInstance(0, 0, dX, dY, 0);
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
				destNode.delVar(key);
				checkAgain = true;
				break;
			}
		}


		// make sure all variables are on the node
		destNode.copyVarsFrom(srcNode);

		// copy any special user bits
		destNode.lowLevelSetUserbits(srcNode.lowLevelGetUserbits());
		destNode.clearExpanded();
        if (srcNode.isExpanded()) destNode.setExpanded();
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
			Variable destVar = destArc.newVar(key, srcVar.getObject(), srcVar.getTextDescriptor());
		}

		// make sure the constraints and other userbits are the same
        destArc.copyPropertiesFrom(srcArc);
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
        private Rectangle2D pasteBounds;
        private double translateX;
        private double translateY;
        private Point2D lastMouseDB;                // last point where mouse was (in db units)
        private JPopupMenu popup;

        /** paste anchor types */

        /**
         * Create a new paste listener
         * @param wnd Controlling window
         * @param pasteList list of objects to paste
         * @param currentListener listener to restore when done
         */
		private PasteListener(EditWindow wnd, List pasteList, EventListener currentListener)
		{
			this.wnd = wnd;
			this.pasteList = pasteList;
			this.currentListener = currentListener;
            this.pasteBounds = getPasteBounds(pasteList);
            translateX = translateY = 0;

            initPopup();

            // get starting point from current mouse location
            Point2D mouse = ClickZoomWireListener.theOne.getLastMouse();
            Point2D mouseDB = wnd.screenToDatabase((int)mouse.getX(), (int)mouse.getY());
            Point2D delta = getDelta(mouseDB, false);

            wnd.getHighlighter().pushHighlight();
            showList(delta);
		}

        /**
         * Gets grid-aligned delta translation for nodes based on mouse location
         * @param mouseDB the location of the mouse
         * @param orthogonal if the translation is orthogonal only
         * @return a grid-aligned delta
         */
        private Point2D getDelta(Point2D mouseDB, boolean orthogonal) {
            EditWindow.gridAlign(mouseDB);
            // this is the point on the clipboard cell that will be pasted at the mouse location
            Point2D refPastePoint = new Point2D.Double(pasteBounds.getCenterX() + translateX,
                                                       pasteBounds.getCenterY() + translateY);

            double deltaX = mouseDB.getX() - refPastePoint.getX();
            double deltaY = mouseDB.getY() - refPastePoint.getY();
            // if orthogonal is true, convert to orthogonal
            if (orthogonal) {
                // only use delta in direction that has larger delta
                if (Math.abs(deltaX) > Math.abs(deltaY)) deltaY = 0;
                else deltaX = 0;
            }
            // this is now a delta, not a point
            refPastePoint.setLocation(deltaX, deltaY);
            EditWindow.gridAlign(refPastePoint);
            return refPastePoint;
        }

        /**
         * Show the objects to paste with the anchor point at 'mouseDB'
         * @param delta the translation for the highlights
         */
		private void showList(Point2D delta)
		{
            // find offset of highlights
            double oX = delta.getX();
            double oY = delta.getY();

			Cell cell = wnd.getCell();
            Highlighter highlighter = wnd.getHighlighter();
			highlighter.clear();
			for(Iterator it = pasteList.iterator(); it.hasNext(); )
			{
                Object obj = it.next();
                Point2D [] points = null;

                if (obj instanceof Variable)
                {
                    Variable var = (Variable)obj;
                    Poly poly = clipCell.computeTextPoly(EditWindow.needCurrent(), var, null);
                    points = poly.getPoints();
                }
                else
                {
                    if (!(obj instanceof Geometric)) continue;
                    Geometric geom = (Geometric)obj;
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
                                    points = Highlighter.describeHighlightText(wnd, geom, var, null);
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
                                highlighter.addLine(new Point2D.Double(fX+oX, fY+oY), new Point2D.Double(tX+oX, tY+oY), cell);
                            }
                            continue;
                        }
                        SizeOffset so = ni.getSizeOffset();
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
                        highlighter.addLine(new Point2D.Double(fX+oX, fY+oY), new Point2D.Double(tX+oX, tY+oY), cell);
                    }
                }
			}
            // show delta from original
            Rectangle2D bounds = wnd.getDisplayedBounds();
            highlighter.addMessage(cell, "("+(int)oX+","+(int)oY+")",
                    new Point2D.Double(bounds.getCenterX(),bounds.getCenterY()));
            // also draw arrow if user has moved highlights off the screen
            double halfWidth = 0.5*pasteBounds.getWidth();
            double halfHeight = 0.5*pasteBounds.getHeight();
            if (Math.abs(translateX) > halfWidth ||
                Math.abs(translateY) > halfHeight) {
                Rectangle2D transBounds = new Rectangle2D.Double(pasteBounds.getX()+oX, pasteBounds.getY()+oY,
                        pasteBounds.getWidth(), pasteBounds.getHeight());
                Poly p = new Poly(transBounds);
                Point2D endPoint = p.closestPoint(lastMouseDB);
                // draw arrow
                highlighter.addLine(lastMouseDB, endPoint, cell);
                int angle = GenMath.figureAngle(lastMouseDB, endPoint);
                angle += 1800;
                int angleOfArrow = 300;		// 30 degrees
                int backAngle1 = angle - angleOfArrow;
                int backAngle2 = angle + angleOfArrow;
                Point2D p1 = new Point2D.Double(endPoint.getX() + DBMath.cos(backAngle1), endPoint.getY() + DBMath.sin(backAngle1));
                Point2D p2 = new Point2D.Double(endPoint.getX() + DBMath.cos(backAngle2), endPoint.getY() + DBMath.sin(backAngle2));
                highlighter.addLine(endPoint, p1, cell);
                highlighter.addLine(endPoint, p2, cell);
            }
			highlighter.finished();
		}

		public void mousePressed(MouseEvent e)
		{
            if (e.isMetaDown()) {
                // right click
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
		}

		public void mouseDragged(MouseEvent evt)
		{
            mouseMoved(evt);
		}

		public void mouseReleased(MouseEvent evt)
		{
            if (evt.isMetaDown()) {
                // right click
                return;
            }
            boolean ctrl = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;
            Point2D mouseDB = wnd.screenToDatabase((int)evt.getX(), (int)evt.getY());
            Point2D delta = getDelta(mouseDB, ctrl);
            showList(delta);

            WindowFrame.setListener(currentListener);
            wnd.getHighlighter().popHighlight();
            PasteObjects job = new PasteObjects(pasteList, delta.getX(), delta.getY());
		}

		public void mouseMoved(MouseEvent evt)
        {
            boolean ctrl = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;
            Point2D mouseDB = wnd.screenToDatabase((int)evt.getX(), (int)evt.getY());
            Point2D delta = getDelta(mouseDB, ctrl);
            lastMouseDB = mouseDB;
            showList(delta);

            wnd.repaint();
        }

		public void mouseClicked(MouseEvent evt) {}
		public void mouseEntered(MouseEvent evt) {}
		public void mouseExited(MouseEvent evt) {}
		public void mouseWheelMoved(MouseWheelEvent e) {}
		public void keyPressed(KeyEvent evt) {
            boolean ctrl = (evt.getModifiersEx()&MouseEvent.CTRL_DOWN_MASK) != 0;
            int chr = evt.getKeyCode();
            if (chr == KeyEvent.VK_ESCAPE) {
                // abort on ESC
                abort();
            }
            else if (chr == KeyEvent.VK_UP) {
                moveObjectsUp();
            }
            else if (chr == KeyEvent.VK_DOWN) {
                moveObjectsDown();
            }
            else if (chr == KeyEvent.VK_LEFT) {
                moveObjectsLeft();
            }
            else if (chr == KeyEvent.VK_RIGHT) {
                moveObjectsRight();
            }
        }
		public void keyReleased(KeyEvent e) {}
		public void keyTyped(KeyEvent e) {}

        private void abort() {
            wnd.getHighlighter().clear();
            wnd.getHighlighter().finished();
            WindowFrame.setListener(currentListener);
            wnd.repaint();
        }

        private void initPopup() {
            popup = new JPopupMenu();
            JMenuItem m;
            m = new JMenuItem("Move objects left");
            m.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0));
            m.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { moveObjectsLeft(); }
            });
            popup.add(m);

            m = new JMenuItem("Move objects right");
            m.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0));
            m.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { moveObjectsRight(); }
            });
            popup.add(m);

            m = new JMenuItem("Move objects up");
            m.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0));
            m.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { moveObjectsUp(); }
            });
            popup.add(m);

            m = new JMenuItem("Move objects down");
            m.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0));
            m.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { moveObjectsDown(); }
            });
            popup.add(m);

            m = new JMenuItem("Abort");
            m.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
            m.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) { abort(); }
            });
            popup.add(m);
        }

        private void moveObjectsLeft() {
            translateX += 0.5*pasteBounds.getWidth();
            Point2D delta = getDelta(lastMouseDB, false);
            showList(delta);
        }
        private void moveObjectsRight() {
            translateX -= 0.5*pasteBounds.getWidth();
            Point2D delta = getDelta(lastMouseDB, false);
            showList(delta);
        }
        private void moveObjectsUp() {
            translateY -= 0.5*pasteBounds.getHeight();
            Point2D delta = getDelta(lastMouseDB, false);
            showList(delta);
        }
        private void moveObjectsDown() {
            translateY += 0.5*pasteBounds.getHeight();
            Point2D delta = getDelta(lastMouseDB, false);
            showList(delta);
        }
	}

}
