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

import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
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

/**
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
	 * There is only one instance of this object (just one clipboard).
	 */
	private Clipboard() {}

	/**
	 * Returns a printable version of this Clipboard.
	 * @return a printable version of this Clipboard.
	 */
	public String toString() { return "Clipboard"; }

	// this is really only for debugging
    public static void editClipboard()
    {
        EditWindow wnd = EditWindow.getCurrent();
        wnd.setCell(clipCell, VarContext.globalContext, null);
    }

    /**
     * Method to copy the selected objects to the clipboard.
     */
	public static void copy()
	{
		// see what is highlighted
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
        List<Geometric> highlightedGeoms = highlighter.getHighlightedEObjs(true, true);
        List<DisplayedText> highlightedText = highlighter.getHighlightedText(true);
        if (highlightedGeoms.size() == 0 && highlightedText.size() == 0)
		{
			System.out.println("First select objects to copy");
			return;
		}

		// special case: if one text object is selected, copy its text to the system clipboard
		copySelectedText(highlightedText);

		// create the transformation for "down in place" copying
        AffineTransform inPlace = new AffineTransform();
        Orientation inPlaceOrient = Orientation.IDENT;
		if (wnd.isInPlaceEdit())
		{
			List<NodeInst> nodes = wnd.getInPlaceEditNodePath();
			for(NodeInst n : nodes)
			{
				Orientation o = n.getOrient().inverse();
				inPlaceOrient = o.concatenate(inPlaceOrient);
			}
			AffineTransform justRotation = inPlaceOrient.pureRotate();

			Rectangle2D pasteBounds = getPasteBounds(highlightedGeoms, highlightedText, wnd);
			AffineTransform untranslate = AffineTransform.getTranslateInstance(-pasteBounds.getCenterX(), -pasteBounds.getCenterY());
			AffineTransform retranslate = AffineTransform.getTranslateInstance(pasteBounds.getCenterX(), pasteBounds.getCenterY());
			inPlace.preConcatenate(untranslate);
			inPlace.preConcatenate(justRotation);
			inPlace.preConcatenate(retranslate);
		}

		// copy to Electric clipboard cell
		new CopyObjects(wnd.getCell(), highlightedGeoms, highlightedText, User.getAlignmentToGrid(),
			inPlace, inPlaceOrient);
	}

    /**
     * Method to copy the selected objects to the clipboard and then delete them.
     */
	public static void cut()
	{
		// see what is highlighted
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
        List<Geometric> highlightedGeoms = highlighter.getHighlightedEObjs(true, true);
        List<DisplayedText> highlightedText = highlighter.getHighlightedText(true);
        if (highlightedGeoms.size() == 0 && highlightedText.size() == 0)
		{
			System.out.println("First select objects to cut");
			return;
		}
        highlighter.clear();
        highlighter.finished();

		// special case: if one text object is selected, copy its text to the system clipboard
		copySelectedText(highlightedText);

		// create the transformation for "down in place" copying
        AffineTransform inPlace = new AffineTransform();
        Orientation inPlaceOrient = Orientation.IDENT;
		if (wnd.isInPlaceEdit())
		{
			List<NodeInst> nodes = wnd.getInPlaceEditNodePath();
			for(NodeInst n : nodes)
			{
				Orientation o = n.getOrient().inverse();
				inPlaceOrient = o.concatenate(inPlaceOrient);
			}
			AffineTransform justRotation = inPlaceOrient.pureRotate();

			Rectangle2D pasteBounds = getPasteBounds(highlightedGeoms, highlightedText, wnd);
			AffineTransform untranslate = AffineTransform.getTranslateInstance(-pasteBounds.getCenterX(), -pasteBounds.getCenterY());
			AffineTransform retranslate = AffineTransform.getTranslateInstance(pasteBounds.getCenterX(), pasteBounds.getCenterY());
			inPlace.preConcatenate(untranslate);
			inPlace.preConcatenate(justRotation);
			inPlace.preConcatenate(retranslate);
		}

		// cut from Electric, copy to clipboard cell
		new CutObjects(wnd.getCell(), highlightedGeoms, highlightedText, User.getAlignmentToGrid(),
			User.isReconstructArcsToDeletedCells(), inPlace, inPlaceOrient);
	}

	/**
	 * Method to paste the clipboard back into the current cell.
	 */
	public static void paste()
	{
		// get objects to paste
		int nTotal = 0, aTotal = 0, vTotal = 0;
        if (clipCell != null)
        {
			nTotal = clipCell.getNumNodes();
			aTotal = clipCell.getNumArcs();
	        vTotal = clipCell.getNumVariables();
	        if (clipCell.getVar(User.FRAME_LAST_CHANGED_BY) !=  null) vTotal--; // discount this variable since it should not be copied.
        }
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
		Cell parent = wnd.getCell();

		// special case of pasting on top of selected objects
		List<Geometric> geoms = highlighter.getHighlightedEObjs(true, true);
		if (geoms.size() > 0)
		{
			// can only paste a single object onto selection
			if (nTotal == 2 && aTotal == 1)
			{
				ArcInst ai = clipCell.getArcs().next();
				NodeInst niHead = ai.getHeadPortInst().getNodeInst();
				NodeInst niTail = ai.getTailPortInst().getNodeInst();
				Iterator<NodeInst> nIt = clipCell.getNodes();
				NodeInst ni1 = nIt.next();
				NodeInst ni2 = nIt.next();
				if ((ni1 == niHead && ni2 == niTail) ||
					(ni1 == niTail && ni2 == niHead)) nTotal = 0;
				total = nTotal + aTotal;
			}
			if (total > 1)
			{
				System.out.println("Can only paste a single object on top of selected objects");
				return;
			}
			for(Geometric geom : geoms)
			{
				if (geom instanceof NodeInst && nTotal == 1)
				{
					NodeInst ni = (NodeInst)geom;
					new PasteNodeToNode(ni, clipCell.getNodes().next());
				} else if (geom instanceof ArcInst && aTotal == 1)
				{
					ArcInst ai = (ArcInst)geom;
					new PasteArcToArc(ai, clipCell.getArcs().next());
				}
			}
			return;
		}

		// make list of things to paste
		List<Geometric> geomList = new ArrayList<Geometric>();
		for(Iterator<NodeInst> it = clipCell.getNodes(); it.hasNext(); )
			geomList.add(it.next());
		for(Iterator<ArcInst> it = clipCell.getArcs(); it.hasNext(); )
			geomList.add(it.next());
		List<DisplayedText> textList = new ArrayList<DisplayedText>();
        for (Iterator<Variable> it = clipCell.getVariables(); it.hasNext(); )
        {
            Variable var = it.next();
            if (!var.isDisplay()) continue;
            textList.add(new DisplayedText(clipCell, var.getKey()));
        }

        if (geomList.size() == 0 && textList.size() == 0) return;

		// create the transformation for "down in place" pasting
        AffineTransform inPlace = new AffineTransform();
        Orientation inPlaceOrient = Orientation.IDENT;
		if (wnd.isInPlaceEdit())
		{
			List<NodeInst> nodes = wnd.getInPlaceEditNodePath();
			for(NodeInst n : nodes)
			{
				Orientation o = n.getOrient();
				inPlaceOrient = inPlaceOrient.concatenate(o);
			}
			AffineTransform justRotation = inPlaceOrient.pureRotate();

			Rectangle2D pasteBounds = getPasteBounds(geomList, textList, wnd);
			AffineTransform untranslate = AffineTransform.getTranslateInstance(-pasteBounds.getCenterX(), -pasteBounds.getCenterY());
			AffineTransform retranslate = AffineTransform.getTranslateInstance(pasteBounds.getCenterX(), pasteBounds.getCenterY());
			inPlace.preConcatenate(untranslate);
			inPlace.preConcatenate(justRotation);
			inPlace.preConcatenate(retranslate);
		}

		if (User.isMoveAfterDuplicate())
		{
			EventListener currentListener = WindowFrame.getListener();
			WindowFrame.setListener(new PasteListener(wnd, geomList, textList, currentListener,
				inPlace, inPlaceOrient));
		} else
		{
		    new PasteObjects(parent, geomList, textList, lastDupX, lastDupY,
		    	User.getAlignmentToGrid(), User.isDupCopiesExports(), User.isArcsAutoIncremented(),
		    	inPlace, inPlaceOrient);
		}
	}

    /**
     * Method to duplicate the selected objects.
     */
    public static void duplicate()
    {
		// see what is highlighted
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd == null) return;
        Highlighter highlighter = wnd.getHighlighter();
        List<Geometric> geomList = highlighter.getHighlightedEObjs(true, true);
		List<DisplayedText> textList = new ArrayList<DisplayedText>();
        for (Iterator<Variable> it = clipCell.getVariables(); it.hasNext(); )
        {
            Variable var = it.next();
            if (!var.isDisplay()) continue;
            textList.add(new DisplayedText(clipCell, var.getKey()));
        }
        if (geomList.size() == 0 && textList.size() == 0)
		{
			System.out.println("First select objects to duplicate");
			return;
		}

		// do duplication
        if (User.isMoveAfterDuplicate())
		{
			EventListener currentListener = WindowFrame.getListener();
			WindowFrame.setListener(new PasteListener(wnd, geomList, textList, currentListener, null, null));
		} else
		{
			new DuplicateObjects(wnd.getCell(), geomList, textList, User.getAlignmentToGrid());
		}
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

	/**
	 * Helper method to copy any selected text to the system-wide clipboard.
	 */
	private static void copySelectedText(List<DisplayedText> highlightedText)
	{
		// must be one text selected
		if (highlightedText.size() != 1) return;

		// get the text
		DisplayedText dt = highlightedText.get(0);
		ElectricObject eObj = dt.getElectricObject();
		Variable.Key varKey = dt.getVariableKey();
		Variable var = eObj.getVar(varKey);
		if (var == null) return;
		String selected = var.describe(-1);
		if (selected == null) return;

		// put the text in the clipboard
		java.awt.datatransfer.Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable transferable = new StringSelection(selected);
		cb.setContents(transferable, null);
	}

	/****************************** CHANGE JOBS ******************************/

	private static class CopyObjects extends Job
	{
		private Cell cell;
        private List<Geometric> highlightedGeoms;
        private List<DisplayedText> highlightedText;
        private double alignment;
        private AffineTransform inPlace;
        private Orientation inPlaceOrient;

		protected CopyObjects(Cell cell, List<Geometric> highlightedGeoms, List<DisplayedText> highlightedText,
			double alignment, AffineTransform inPlace, Orientation inPlaceOrient)
		{
			super("Copy", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.highlightedGeoms = highlightedGeoms;
            this.highlightedText = highlightedText;
            this.alignment = alignment;
            this.inPlace = inPlace;
            this.inPlaceOrient = inPlaceOrient;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// remove contents of clipboard
			clear();

			// copy objects to clipboard
			copyListToCell(clipCell, highlightedGeoms, highlightedText, null, null,
				new Point2D.Double(0,0), User.isDupCopiesExports(), User.isArcsAutoIncremented(),
				alignment, inPlace, inPlaceOrient);
			return true;
		}
	}

	private static class CutObjects extends Job
	{
		private Cell cell;
        private List<Geometric> geomList;
        private List<DisplayedText> textList;
        private double alignment;
        private boolean reconstructArcs;
        private AffineTransform inPlace;
        private Orientation inPlaceOrient;

		protected CutObjects(Cell cell, List<Geometric> geomList, List<DisplayedText> textList, double alignment,
			boolean reconstructArcs, AffineTransform inPlace, Orientation inPlaceOrient)
		{
			super("Cut", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.geomList = geomList;
            this.textList = textList;
            this.alignment = alignment;
            this.reconstructArcs = reconstructArcs;
            this.inPlace = inPlace;
            this.inPlaceOrient = inPlaceOrient;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// remove contents of clipboard
			clear();

			// make sure deletion is allowed
			if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return false;
			List<Highlight2> deleteList = new ArrayList<Highlight2>();
			for(Geometric geom : geomList)
			{
				if (geom instanceof NodeInst)
				{
					int errorCode = CircuitChangeJobs.cantEdit(cell, (NodeInst)geom, true);
					if (errorCode < 0) return false;
					if (errorCode > 0) continue;
				}
			}

			// copy objects to clipboard
			copyListToCell(clipCell, geomList, textList, null, null,
				new Point2D.Double(0, 0), User.isDupCopiesExports(), User.isArcsAutoIncremented(),
				alignment, inPlace, inPlaceOrient);

			// and delete the original objects
			CircuitChangeJobs.eraseObjectsInList(cell, geomList, reconstructArcs);

			// kill variables on cells
            for(DisplayedText dt : textList)
            {
                ElectricObject owner = dt.getElectricObject();
                if (!(owner instanceof Cell)) continue;                
                owner.delVar(dt.getVariableKey());
            }
			return true;
		}
	}

	private static class DuplicateObjects extends Job
    {
		private Cell cell;
		private List<Geometric> geomList, newGeomList;
		private List<DisplayedText> textList, newTextList;
		private double alignment;
		private NodeInst lastCreatedNode;

        protected DuplicateObjects(Cell cell, List<Geometric> geomList, List<DisplayedText> textList, double alignment)
        {
            super("Duplicate", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.geomList = geomList;
            this.textList = textList;
            this.alignment = alignment;
            startJob();
        }

        public boolean doIt() throws JobException
        {
            // copy objects to clipboard
			newGeomList = new ArrayList<Geometric>();
			newTextList = new ArrayList<DisplayedText>();
			lastCreatedNode = copyListToCell(cell, geomList, textList, newGeomList, newTextList,
            	new Point2D.Double(lastDupX, lastDupY), User.isDupCopiesExports(), User.isArcsAutoIncremented(),
            	alignment, null, null);
			fieldVariableChanged("newGeomList");
			fieldVariableChanged("newTextList");
			fieldVariableChanged("lastCreatedNode");
			return true;
        }

		public void terminateOK()
		{
			// remember the last node created
			lastDup = lastCreatedNode;

			// highlight the copy
			showCopiedObjects(newGeomList, newTextList);
		}
    }

	private static class PasteArcToArc extends Job
	{
		private ArcInst src, dst, newArc;

		protected PasteArcToArc(ArcInst dst, ArcInst src)
		{
			super("Paste Arc to Arc", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.src = src;
			this.dst = dst;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure pasting is allowed
			if (CircuitChangeJobs.cantEdit(dst.getParent(), null, true) != 0) return false;

			newArc = pasteArcToArc(dst, src);
			if (newArc == null) System.out.println("Nothing was pasted"); else
				fieldVariableChanged("newArc");
			return true;
		}

        public void terminateOK()
        {
            if (newArc != null)
            {
            	EditWindow wnd = EditWindow.getCurrent();
            	if (wnd != null)
            	{
            		Highlighter highlighter = wnd.getHighlighter();
            		if (highlighter != null)
            		{
		                highlighter.clear();
		                highlighter.addElectricObject(newArc, newArc.getParent());
		                highlighter.finished();
            		}
            	}
            };
        }
	}

	private static class PasteNodeToNode extends Job
	{
		private NodeInst src, dst, newNode;

		protected PasteNodeToNode(NodeInst dst, NodeInst src)
		{
			super("Paste Node to Node", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.src = src;
			this.dst = dst;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure pasting is allowed
			if (CircuitChangeJobs.cantEdit(dst.getParent(), null, true) != 0) return false;

			newNode = pasteNodeToNode(dst, src);
			if (newNode == null) System.out.println("Nothing was pasted"); else
				fieldVariableChanged("newNode");
			return true;
		}

        public void terminateOK()
        {
            if (newNode != null)
            {
            	EditWindow wnd = EditWindow.getCurrent();
            	if (wnd != null)
            	{
            		Highlighter highlighter = wnd.getHighlighter();
            		if (highlighter != null)
            		{
		                highlighter.clear();
		                highlighter.addElectricObject(newNode, newNode.getParent());
		                highlighter.finished();
            		}
            	}
            }
        }
	}

	private static class PasteObjects extends Job
	{
		private Cell cell;
		private List<Geometric> geomList, newGeomList;
		private List<DisplayedText> textList, newTextList;
		private double dX, dY, alignment;
		private boolean copyExports, uniqueArcs;
		private NodeInst lastCreatedNode;
		private AffineTransform inPlace;
		private Orientation inPlaceOrient;

		protected PasteObjects(Cell cell, List<Geometric> geomList, List<DisplayedText> textList,
			double dX, double dY, double alignment, boolean copyExports, boolean uniqueArcs,
			AffineTransform inPlace, Orientation inPlaceOrient)
		{
			super("Paste", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.geomList = geomList;
			this.textList = textList;
			this.dX = dX;
			this.dY = dY;
			this.alignment = alignment;
			this.copyExports = copyExports;
			this.uniqueArcs = uniqueArcs;
			this.inPlace = inPlace;
			this.inPlaceOrient = inPlaceOrient;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// make sure pasting is allowed
			if (CircuitChangeJobs.cantEdit(cell, null, true) != 0) return false;

			// paste them into the current cell
			newGeomList = new ArrayList<Geometric>();
			newTextList = new ArrayList<DisplayedText>();
			lastCreatedNode = copyListToCell(cell, geomList, textList, newGeomList, newTextList,
				new Point2D.Double(dX, dY), copyExports, uniqueArcs, alignment, inPlace, inPlaceOrient);
			fieldVariableChanged("newGeomList");
			fieldVariableChanged("newTextList");
			fieldVariableChanged("lastCreatedNode");
			return true;
		}

		public void terminateOK()
		{
			// remember the last node created
			lastDup = lastCreatedNode;

			// highlight the copy
			showCopiedObjects(newGeomList, newTextList);
		}
	}

	/****************************** CHANGE JOB SUPPORT ******************************/

	/**
	 * Method to clear the clipboard.
	 */
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
		List<ArcInst> arcsToDelete = new ArrayList<ArcInst>();
		for(Iterator<ArcInst> it = clipCell.getArcs(); it.hasNext(); )
			arcsToDelete.add(it.next());
		for(ArcInst ai : arcsToDelete)
		{
			ai.kill();
		}

		// delete all exports in the clipboard
		List<Export> exportsToDelete = new ArrayList<Export>();
		for(Iterator<Export> it = clipCell.getExports(); it.hasNext(); )
			exportsToDelete.add(it.next());
		for(Export pp : exportsToDelete)
		{
			pp.kill();
		}

		// delete all nodes in the clipboard
		List<NodeInst> nodesToDelete = new ArrayList<NodeInst>();
		for(Iterator<NodeInst> it = clipCell.getNodes(); it.hasNext(); )
			nodesToDelete.add(it.next());
		for(NodeInst ni : nodesToDelete)
		{
			ni.kill();
		}

        // Delete all variables
        List<Variable> varsToDelete = new ArrayList<Variable>();
        for(Iterator<Variable> it = clipCell.getVariables(); it.hasNext(); )
		{
			Variable var = it.next();
            clipCell.delVar(var.getKey());
			//varsToDelete.add(var);
		}
	}

	/**
	 * Method to copy the list of Geometrics to a new Cell.
	 * @param toCell the destination cell of the Geometrics.
	 * @param geomList the list of Geometrics to copy.
	 * @param textList the list of text to copy.
	 * @param newGeomList the list of Geometrics that were created.
	 * @param newTextList the list of text objects that were created.
	 * @param delta an offset for all of the copied Geometrics.
	 * @param copyExports true to copy exports.
	 * @param uniqueArcs true to generate unique arc names.
	 * @param alignment the grid alignment to use (0 for none).
	 * @param inPlace the transformation to use which accounts for "down in place" editing.
	 * @param inPlaceOrient the orientation to use which accounts for "down in place" editing.
	 * @return the last NodeInst that was created.
	 */
	public static NodeInst copyListToCell(Cell toCell, List<Geometric> geomList, List<DisplayedText> textList,
		List<Geometric> newGeomList, List<DisplayedText> newTextList, Point2D delta, boolean copyExports,
		boolean uniqueArcs, double alignment, AffineTransform inPlace, Orientation inPlaceOrient)
	{
        // make a list of all objects to be copied (includes end points of arcs)
        List<NodeInst> theNodes = new ArrayList<NodeInst>();
        List<ArcInst> theArcs = new ArrayList<ArcInst>();
        for (Geometric geom : geomList)
        {
            if (geom instanceof NodeInst)
            {
                if (!theNodes.contains(geom)) theNodes.add((NodeInst)geom);
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
		if (theNodes.size() == 0 && textList.size() == 0) return null;

		// check for recursion
		for(NodeInst ni : theNodes)
		{
			if (!ni.isCellInstance()) continue;
			Cell niCell = (Cell)ni.getProto();
            if (Cell.isInstantiationRecursive(niCell, toCell))
			{
				System.out.println("Cannot: that would be recursive (" +
					toCell + " is beneath " + ni.getProto() + ")");
				return null;
			}
		}

		DBMath.gridAlign(delta, alignment);
        double dX = delta.getX();
        double dY = delta.getY();

		// sort the nodes by name
		Collections.sort(theNodes);

		// create the new nodes
		NodeInst lastCreatedNode = null;
		HashMap<NodeInst,NodeInst> newNodes = new HashMap<NodeInst,NodeInst>();
        List<PortInst> portInstsToExport = new ArrayList<PortInst>();
        HashMap<PortInst,Export> originalExports = new HashMap<PortInst,Export>();
		for(NodeInst ni : theNodes)
		{
			if (ni.getProto() == Generic.tech.cellCenterNode && toCell.alreadyCellCenter()) continue;
			double width = ni.getXSize();
			double height = ni.getYSize();
			String name = null;
			if (ni.isUsernamed())
				name = ElectricObject.uniqueObjectName(ni.getName(), toCell, NodeInst.class, false);
            EPoint point = new EPoint(ni.getAnchorCenterX()+dX, ni.getAnchorCenterY()+dY);
            Orientation orient = ni.getOrient();
            if (inPlace != null)
            {
            	Point2D dst = new Point2D.Double(0, 0);
            	inPlace.transform(new Point2D.Double(ni.getAnchorCenterX(), ni.getAnchorCenterY()), dst);
            	point = new EPoint(dst.getX()+dX, dst.getY()+dY);
            	orient = orient.concatenate(inPlaceOrient);
            }
			NodeInst newNi = NodeInst.newInstance(ni.getProto(), point, width, height,
				toCell, orient, name, ni.getTechSpecific());
			if (newNi == null)
			{
				System.out.println("Cannot create node");
				return lastCreatedNode;
			}
			newNi.copyStateBits(ni);
			newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_PROTO);
			newNi.copyTextDescriptorFrom(ni, NodeInst.NODE_NAME);
			newNi.copyVarsFrom(ni);
			newNodes.put(ni, newNi);
			if (newGeomList != null) newGeomList.add(newNi);
			lastCreatedNode = newNi;

			// copy the ports, too
			if (copyExports)
			{
				for(Iterator<Export> eit = ni.getExports(); eit.hasNext(); )
				{
					Export pp = eit.next();
                    PortInst pi = ExportChanges.getNewPortFromReferenceExport(newNi, pp);
                    portInstsToExport.add(pi);
					originalExports.put(pi, pp);
				}
			}
		}
		if (copyExports)
			ExportChanges.reExportPorts(toCell, portInstsToExport, true, true, false, originalExports);

		HashMap<ArcInst,ArcInst> newArcs = new HashMap<ArcInst,ArcInst>();
		if (theArcs.size() > 0)
		{
			// sort the arcs by name
			Collections.sort(theArcs);

			// for associating old names with new names
			HashMap<String,String> newArcNames = new HashMap<String,String>();

			AffineTransform fixOffset = null;
			if (inPlaceOrient != null) fixOffset = inPlaceOrient.pureRotate();

			// create the new arcs
			for(ArcInst ai : theArcs)
			{
				PortInst oldHeadPi = ai.getHeadPortInst();
				NodeInst headNi = newNodes.get(oldHeadPi.getNodeInst());
				PortInst headPi = headNi.findPortInstFromProto(oldHeadPi.getPortProto());
                EPoint headP = oldHeadPi.getCenter();
				double headDX = ai.getHeadLocation().getX() - headP.getX();
				double headDY = ai.getHeadLocation().getY() - headP.getY();

				PortInst oldTailPi = ai.getTailPortInst();
				NodeInst tailNi = newNodes.get(oldTailPi.getNodeInst());
				PortInst tailPi = tailNi.findPortInstFromProto(oldTailPi.getPortProto());
                EPoint tailP = oldTailPi.getCenter();
				double tailDX = ai.getTailLocation().getX() - tailP.getX();
				double tailDY = ai.getTailLocation().getY() - tailP.getY();

				// adjust offset if down-in-place
				if (fixOffset != null)
				{
					Point2D result = new Point2D.Double(0, 0);
					fixOffset.transform(new Point2D.Double(headDX, headDY), result);
					headDX = result.getX();
					headDY = result.getY();
					fixOffset.transform(new Point2D.Double(tailDX, tailDY), result);
					tailDX = result.getX();
					tailDY = result.getY();
				}

				String name = null;
				if (ai.isUsernamed())
				{
					name = ai.getName();
					if (uniqueArcs)
					{
						String newName = newArcNames.get(name);
						if (newName == null)
						{
							newName = ElectricObject.uniqueObjectName(name, toCell, ArcInst.class, false);
							newArcNames.put(name, newName);
						}
						name = newName;
					}
				}
				headP = new EPoint(headPi.getCenter().getX() + headDX, headPi.getCenter().getY() + headDY);
				tailP = new EPoint(tailPi.getCenter().getX() + tailDX, tailPi.getCenter().getY() + tailDY);
				ArcInst newAr = ArcInst.newInstance(ai.getProto(), ai.getWidth(),
					headPi, tailPi, headP, tailP, name, ai.getAngle());
				if (newAr == null)
				{
					System.out.println("Cannot create arc");
					return lastCreatedNode;
				}
				newAr.copyPropertiesFrom(ai);
				newArcs.put(ai, newAr);
				if (newGeomList != null) newGeomList.add(newAr);
			}
		}

		// copy variables on cells
        for(DisplayedText dt : textList)
		{
        	ElectricObject eObj = dt.getElectricObject();
        	if (!(eObj instanceof Cell)) continue;
			Variable.Key varKey = dt.getVariableKey();
			Variable var = eObj.getVar(varKey);
			double xP = var.getTextDescriptor().getXOff();
			double yP = var.getTextDescriptor().getYOff();
			Variable newv = toCell.newVar(varKey, var.getObject(), var.getTextDescriptor().withOff(xP+dX, yP+dY));
			if (newTextList != null) newTextList.add(new DisplayedText(toCell, varKey));
		}
        return lastCreatedNode;
	}

	private static void showCopiedObjects(List<Geometric> newGeomList, List<DisplayedText> newTextList)
	{
		EditWindow wnd = EditWindow.needCurrent();
		if (wnd != null)
		{
			Cell cell = wnd.getCell();
			Highlighter highlighter = wnd.getHighlighter();
			highlighter.clear();
			for(Geometric geom : newGeomList)
			{
				if (geom instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)geom;
	
					// special case for displayable text on invisible pins
					if (ni.isInvisiblePinWithText())
					{
						Poly [] polys = ni.getAllText(false, wnd);
						if (polys == null) continue;
						for(int i=0; i<polys.length; i++)
						{
							Poly poly = polys[i];
							if (poly == null) continue;
							Highlight2 h = highlighter.addText(ni, cell, poly.getDisplayedText().getVariableKey());
						}
						continue;
					}
				}
				highlighter.addElectricObject(geom, cell);
			}
			for(DisplayedText dt : newTextList)
			{
				highlighter.addText(dt.getElectricObject(), cell, dt.getVariableKey());
			}
			highlighter.finished();
		}
	}

	/**
	 * Method to "paste" node "srcnode" onto node "destnode", making them the same.
	 * Returns the address of the destination node (null on error).
	 */
	private static NodeInst pasteNodeToNode(NodeInst destNode, NodeInst srcNode)
	{
		destNode = CircuitChangeJobs.replaceNodeInst(destNode, srcNode.getProto(), true, false);
		if (destNode == null) return null;

		destNode.clearExpanded();
		if (srcNode.isExpanded()) destNode.setExpanded();

		if (!destNode.isCellInstance() && !srcNode.isCellInstance()) {
			if (srcNode.getProto().getTechnology() == destNode.getProto().getTechnology()) {
				Technology tech = srcNode.getProto().getTechnology();
				tech.setPrimitiveFunction(destNode, srcNode.getFunction());
			}
		}

		// make the sizes the same if they are primitives
		if (!destNode.isCellInstance())
		{
			double dX = srcNode.getXSize() - destNode.getXSize();
			double dY = srcNode.getYSize() - destNode.getYSize();
			if (dX != 0 || dY != 0)
			{
				destNode.resize(dX, dY);
			}
		}

		// remove variables that are not on the pasted object
		boolean checkAgain = true;
		while (checkAgain)
		{
			checkAgain = false;
			for(Iterator<Variable> it = destNode.getVariables(); it.hasNext(); )
			{
				Variable destVar = it.next();
				Variable.Key key = destVar.getKey();
				Variable srcVar = srcNode.getVar(key);
				if (srcVar != null) continue;
				destNode.delVar(key);
				checkAgain = true;
				break;
			}
		}


		// make sure all variables are on the node
		destNode.copyVarsFrom(srcNode);

		// copy any special user bits
		destNode.copyStateBits(srcNode);
		destNode.clearExpanded();
		if (srcNode.isExpanded()) destNode.setExpanded();
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
			for(Iterator<Variable> it = destArc.getVariables(); it.hasNext(); )
			{
				Variable destVar = it.next();
				Variable.Key key = destVar.getKey();
				Variable srcVar = srcArc.getVar(key);
				if (srcVar != null) continue;
				destArc.delVar(key);
				checkAgain = true;
				break;
			}
		}

		// make sure all variables are on the arc
		for(Iterator<Variable> it = srcArc.getVariables(); it.hasNext(); )
		{
			Variable srcVar = it.next();
			Variable.Key key = srcVar.getKey();
			Variable destVar = destArc.newVar(key, srcVar.getObject(), srcVar.getTextDescriptor());
		}

		// make sure the constraints and other userbits are the same
		destArc.copyPropertiesFrom(srcArc);
		return destArc;
	}

    /**
     * Gets a boundary representing the paste bounds of the list of objects.
     * The corners and center point of the bounds can be used as anchors
     * when pasting the objects interactively. This is all done in database units.
     * Note: you will likely want to grid align any points before using them.
     * @param pasteList a list of Geometrics to paste
     * @return a Rectangle2D that is the paste bounds.
     */
    private static Rectangle2D getPasteBounds(List<Geometric> geomList, List<DisplayedText> textList, EditWindow wnd) {

        Point2D llcorner = null;
        Point2D urcorner = null;

        // figure out lower-left corner and upper-rigth corner of this collection of objects
        for(DisplayedText dt : textList)
        {
        	ElectricObject eObj = dt.getElectricObject();
            Poly poly = clipCell.computeTextPoly(wnd, dt.getVariableKey());
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
        for(Geometric geom : geomList)
        {
            if (geom instanceof NodeInst)
            {
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
            } else
            {
                ArcInst ai = (ArcInst)geom;
                double wid = ai.getWidth() - ai.getProto().getWidthOffset();
                Poly poly = ai.makePoly(wid, Poly.Type.FILLED);
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

        // figure bounds
        double width = urcorner.getX() - llcorner.getX();
        double height = urcorner.getY() - llcorner.getY();
        Rectangle2D bounds = new Rectangle2D.Double(llcorner.getX(), llcorner.getY(), width, height);
        return bounds;
    }

	/****************************** PASTE LISTENER ******************************/

	/**
	 * Class to handle the interactive drag after a paste.
	 */
	private static class PasteListener
		implements MouseMotionListener, MouseListener, MouseWheelListener, KeyListener
	{
		private EditWindow wnd;
		private List<Geometric> geomList;
		private List<DisplayedText> textList;
		private EventListener currentListener;
		private Rectangle2D pasteBounds;
		private double translateX;
		private double translateY;
		private Point2D lastMouseDB;				// last point where mouse was (in db units)
		private JPopupMenu popup;
		private AffineTransform inPlace;
		private Orientation inPlaceOrient;

		/** paste anchor types */

		/**
		 * Create a new paste listener
		 * @param wnd Controlling window
		 * @param pasteList list of objects to paste
		 * @param currentListener listener to restore when done
		 */
		private PasteListener(EditWindow wnd, List<Geometric> geomList, List<DisplayedText> textList,
			EventListener currentListener, AffineTransform inPlace, Orientation inPlaceOrient)
		{
			this.wnd = wnd;
			this.geomList = geomList;
			this.textList = textList;
			this.currentListener = currentListener;
			this.inPlace = inPlace;
			this.inPlaceOrient = inPlaceOrient;
			this.pasteBounds = getPasteBounds(geomList, textList, wnd);
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
		private Point2D getDelta(Point2D mouseDB, boolean orthogonal)
		{
			// mouseDB == null if you press arrow keys before placing the new copy
			if (mouseDB == null) return null;
			double alignment = User.getAlignmentToGrid();
			DBMath.gridAlign(mouseDB, alignment);

			// this is the point on the clipboard cell that will be pasted at the mouse location
			Point2D refPastePoint = new Point2D.Double(pasteBounds.getCenterX() + translateX,
													   pasteBounds.getCenterY() + translateY);

			double deltaX = mouseDB.getX() - refPastePoint.getX();
			double deltaY = mouseDB.getY() - refPastePoint.getY();

			// if orthogonal is true, convert to orthogonal
			if (orthogonal)
			{
				// only use delta in direction that has larger delta
				if (Math.abs(deltaX) > Math.abs(deltaY)) deltaY = 0;
					else deltaX = 0;
			}

			// this is now a delta, not a point
			refPastePoint.setLocation(deltaX, deltaY);
			DBMath.gridAlign(refPastePoint, alignment);
			return refPastePoint;
		}

		/**
		 * Show the objects to paste with the anchor point at 'mouseDB'
		 * @param delta the translation for the highlights
		 */
		private void showList(Point2D delta)
		{
			// if delta==null, problems to get mouseDB pointer
			if (delta == null) return;

			// find offset of highlights
			double oX = delta.getX();
			double oY = delta.getY();

			Cell cell = wnd.getCell();
			Highlighter highlighter = wnd.getHighlighter();
			highlighter.clear();
			for(Geometric geom : geomList)
			{
				if (geom instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)geom;
					Poly poly = ai.makePoly(ai.getWidth() - ai.getProto().getWidthOffset(), Poly.Type.CLOSED);
					if (inPlace != null) poly.transform(inPlace);
					Point2D [] points = poly.getPoints();
					showPoints(points, oX, oY, cell, highlighter);
					continue;
				}

				NodeInst ni = (NodeInst)geom;
				if (ni.isInvisiblePinWithText())
				{
					// find text on the invisible pin
					boolean found = false;
					for(Iterator<Variable> vIt = ni.getVariables(); vIt.hasNext(); )
					{
						Variable var = vIt.next();
						if (var.isDisplay())
						{
							Point2D [] points = Highlighter.describeHighlightText(wnd, geom, var.getKey());
							if (inPlace != null) inPlace.transform(points, 0, points, 0, points.length);
							showPoints(points, oX, oY, cell, highlighter);
							found = true;
							break;
						}
					}
					if (found) continue;
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
				if (inPlace != null) poly.transform(inPlace);
				poly.transform(trans);
				showPoints(poly.getPoints(), oX, oY, cell, highlighter);
			}

			// show delta from original
			Rectangle2D bounds = wnd.getDisplayedBounds();
			highlighter.addMessage(cell, "("+(int)oX+","+(int)oY+")",
					new Point2D.Double(bounds.getCenterX(),bounds.getCenterY()));

			// also draw arrow if user has moved highlights off the screen
			double halfWidth = 0.5*pasteBounds.getWidth();
			double halfHeight = 0.5*pasteBounds.getHeight();
			if (Math.abs(translateX) > halfWidth || Math.abs(translateY) > halfHeight)
			{
				Rectangle2D transBounds = new Rectangle2D.Double(pasteBounds.getX()+oX, pasteBounds.getY()+oY,
					pasteBounds.getWidth(), pasteBounds.getHeight());
				Poly p = new Poly(transBounds);
				if (inPlace != null) p.transform(inPlace);
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

		private void showPoints(Point2D [] points, double oX, double oY, Cell cell, Highlighter highlighter)
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
			Cell cell = WindowFrame.needCurCell();
			if (cell != null)
				new PasteObjects(cell, geomList, textList, delta.getX(), delta.getY(),
					User.getAlignmentToGrid(), User.isDupCopiesExports(), User.isArcsAutoIncremented(),
					inPlace, inPlaceOrient);
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
