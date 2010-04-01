/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: User.java
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
package com.sun.electric.tool.user;

import com.sun.electric.StartupPrefs;
import com.sun.electric.database.IdMapper;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Xml;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.AbstractUserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.ToolSettings;
import com.sun.electric.tool.user.redisplay.VectorCache;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.swing.SwingUtilities;

/**
 * This is the User Interface tool.
 */
public class User extends Listener
{
	// ---------------------- private and protected methods -----------------

	/** the User Interface tool. */		private static User tool = new User();
	/** key of Variable holding cell frame information. */				public static final Variable.Key FRAME_SIZE = Variable.newKey("FACET_schematic_page_size");
	/** key of Variable holding cell company name. */					public static final Variable.Key FRAME_COMPANY_NAME = Variable.newKey("USER_drawing_company_name");
	/** key of Variable holding cell designer name. */					public static final Variable.Key FRAME_DESIGNER_NAME = Variable.newKey("USER_drawing_designer_name");
	/** key of Variable holding user who last changed the cell. */		public static final Variable.Key FRAME_LAST_CHANGED_BY = Variable.newKey("USER_drawing_last_changed_by");
	/** key of Variable holding cell project name. */					public static final Variable.Key FRAME_PROJECT_NAME = Variable.newKey("USER_drawing_project_name");

	private ArcProto currentArcProto = null;
    private Technology currentTech = null;
    private Map<String,PrimitivePort> currentContactPortProtoMap = new HashMap<String,PrimitivePort>();
    private Map<String,List<PrimitivePort>> equivalentPortProtoMap = new HashMap<String,List<PrimitivePort>>();
//	private NodeProto currentNodeProto = null;
//	private boolean undoRedo;

	/**
	 * The constructor sets up the User tool.
	 */
	private User()
	{
		super("user");
	}

	/**
	 * Method to initialize the User Interface tool.
	 */
	public void init()
	{
		// the user interface tool is always on
		setOn();
		setIncremental();

//		if (Job.getRunMode() != Job.Mode.CLIENT)
//			Clipboard.clear(); // To initialize Clibpoard Cell
	}

	/**
	 * Method to retrieve the singleton associated with the User tool.
	 * @return the User tool.
	 */
	public static User getUserTool() { return tool; }

//	/**
//	 * Method to handle a change to a NodeInst.
//	 * @param ni the NodeInst that was changed.
//	 * @param oldD the old contents of the NodeInst.
//	 */
//	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oldD)
//	{
//		Clipboard.nodeMoved(ni, oldD.anchor.getX(), oldD.anchor.getY());
//
//		// remember what has changed in the cell
//		Cell cell = ni.getParent();
//		Rectangle2D.Double oldBounds = new Rectangle2D.Double();
//		oldD.computeBounds(ni, oldBounds);
//		Rectangle2D.Double newBounds = new Rectangle2D.Double();
//		ni.getD().computeBounds(ni, newBounds);	// TO DO Why can't we use "ni.getBounds()" ?
//		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//		{
//			WindowFrame wf = it.next();
//			if (wf.getContent() instanceof EditWindow)
//			{
//				EditWindow wnd = (EditWindow)wf.getContent();
//				if (wnd.getCell() == cell)
//				{
//					// TO DO figure out way to find text bounds on the OLD object
//					setChangedInWindow(wnd, oldBounds);
//
//					// figure out full bounds including text
//					Rectangle2D newTextBounds = ni.getTextBounds(wnd);
//					if (newTextBounds == null) setChangedInWindow(wnd, newBounds); else
//					{
//						Rectangle2D.union(newTextBounds, newBounds, newTextBounds);
//						setChangedInWindow(wnd, newTextBounds);
//					}
//				}
//			}
//		}
//	}
//
//	/**
//	 * Method to handle a change to an ArcInst.
//	 * @param ai the ArcInst that changed.
//	 * @param oD the old contents of the ArcInst.
//	 */
//	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD)
//	{
//		// remember what has changed in the cell
//		Cell cell = ai.getParent();
//		Poly oldPoly = ArcInst.makePolyForArc(ai, oD.length, oD.width, oD.headLocation, oD.tailLocation, Poly.Type.FILLED);
//		Rectangle2D oldBounds = oldPoly.getBounds2D();
//		Rectangle2D newBounds = ai.getBounds();
//		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//		{
//			WindowFrame wf = it.next();
//			if (wf.getContent() instanceof EditWindow)
//			{
//				EditWindow wnd = (EditWindow)wf.getContent();
//				if (wnd.getCell() == cell)
//				{
//					// TO DO figure out way to find text bounds on the OLD object
//					setChangedInWindow(wnd, oldBounds);
//
//					// figure out full bounds including text
//					Rectangle2D newTextBounds = ai.getTextBounds(wnd);
//					if (newTextBounds == null) setChangedInWindow(wnd, newBounds); else
//					{
//						Rectangle2D.union(newTextBounds, newBounds, newTextBounds);
//						setChangedInWindow(wnd, newTextBounds);
//					}
//				}
//			}
//		}
//	}
//
//	/**
//	 * Method to handle a change to an Export.
//	 * @param pp the Export that moved.
//	 * @param oD the old contents of the Export.
//	 */
//	public void modifyExport(Export pp, ImmutableExport oD)
//	{
//		PortInst oldPi = ((Cell)pp.getParent()).getPortInst(oD.originalNodeId, oD.originalPortId);
//		// remember what has changed in the cell
//		Cell cell = (Cell)pp.getParent();
//		NodeInst oldNi = oldPi.getNodeInst();
//		NodeInst newNi = pp.getOriginalPort().getNodeInst();
//		Rectangle2D oldBounds = oldPi.getBounds();
//		Rectangle2D newBounds = newNi.getBounds();
//		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//		{
//			WindowFrame wf = it.next();
//			if (wf.getContent() instanceof EditWindow)
//			{
//				EditWindow wnd = (EditWindow)wf.getContent();
//				if (wnd.getCell() == cell)
//				{
//					// figure out full bounds including text
//					Rectangle2D oldTextBounds = oldNi.getTextBounds(wnd);
//					if (oldTextBounds == null) setChangedInWindow(wnd, oldBounds); else
//					{
//						Rectangle2D.union(oldTextBounds, oldBounds, oldTextBounds);
//						setChangedInWindow(wnd, oldTextBounds);
//					}
//
//					// figure out full bounds including text
//					Rectangle2D newTextBounds = newNi.getTextBounds(wnd);
//					if (newTextBounds == null) setChangedInWindow(wnd, newBounds); else
//					{
//						Rectangle2D.union(newTextBounds, newBounds, newTextBounds);
//						setChangedInWindow(wnd, newTextBounds);
//					}
//				}
//			}
//		}
//	}
//
//	/**
//	 * Method to handle a change to a Cell.
//	 * @param cell the Cell that was changed.
//	 * @param oD the old contents of the Cell.
//	 */
//	public void modifyCell(Cell cell, ImmutableCell oD) {
//		redrawObject(cell);
//		CellName oldCellName = oD.cellName;
//		if (cell.getCellName() == oldCellName) return;
//		if (cell.isInTechnologyLibrary()) {
//			Manipulate.renamedCell(oldCellName.getName(), cell.getName());
//		}
//		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
//			WindowFrame wf = it.next();
//			WindowContent content = wf.getContent();
//			if (content.getCell() != cell) continue;
//			content.setWindowTitle();
//		}
//	}
//
//	/**
//	 * Method to handle a change to a Library.
//	 * @param lib the Library that was changed.
//	 * @param oldD the old contents of the Library.
//	 */
//	public void modifyLibrary(Library lib, ImmutableLibrary oldD) {}
//
//	/**
//	 * Method to handle the creation of a new ElectricObject.
//	 * @param obj the ElectricObject that was just created.
//	 */
//	public void newObject(ElectricObject obj)
//	{
//		// remember what has changed in the cell
//		Cell cell = null;
//		Rectangle2D bounds = null;
//		if (obj instanceof NodeInst)
//		{
//			NodeInst ni = (NodeInst)obj;
//			cell = ni.getParent();
//			bounds = ni.getBounds();
//		} else if (obj instanceof ArcInst)
//		{
//			ArcInst ai = (ArcInst)obj;
//			cell = ai.getParent();
//			bounds = ai.getBounds();
//		} else if (obj instanceof Export)
//		{
//			Export pp = (Export)obj;
//			cell = (Cell)pp.getParent();
//			bounds = pp.getOriginalPort().getNodeInst().getBounds();
//		}
//		if (cell == null) return;
//		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//		{
//			WindowFrame wf = it.next();
//			if (wf.getContent() instanceof EditWindow)
//			{
//				EditWindow wnd = (EditWindow)wf.getContent();
//				if (wnd.getCell() == cell)
//				{
//					// figure out full bounds including text
//					Rectangle2D textBounds = obj.getTextBounds(wnd);
//					if (textBounds == null) setChangedInWindow(wnd, bounds); else
//					{
//						Rectangle2D.union(textBounds, bounds, textBounds);
//						setChangedInWindow(wnd, bounds);
//					}
//				}
//			}
//		}
//	}
//
//	/**
//	 * Method to handle the deletion of an ElectricObject.
//	 * @param obj the ElectricObject that was just deleted.
//	 */
//	public void killObject(ElectricObject obj)
//	{
//		if (obj instanceof Cell)
//		{
//			Cell cell = (Cell)obj;
//			if (cell.isInTechnologyLibrary())
//			{
//				Manipulate.deletedCell(cell);
//			}
//			return;
//		}
//
//		// remember what has changed in the cell
//		// remember what has changed in the cell
//		Cell cell = null;
//		Rectangle2D bounds = null;
//		if (obj instanceof NodeInst)
//		{
//			NodeInst ni = (NodeInst)obj;
//			cell = ni.getParent();
//			bounds = ni.getBounds();
//		} else if (obj instanceof ArcInst)
//		{
//			ArcInst ai = (ArcInst)obj;
//			cell = ai.getParent();
//			bounds = ai.getBounds();
//		} else if (obj instanceof Export)
//		{
//			Export pp = (Export)obj;
//			cell = (Cell)pp.getParent();
//			bounds = pp.getOriginalPort().getNodeInst().getBounds();
//		}
//		if (cell == null) return;
//		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//		{
//			WindowFrame wf = it.next();
//			if (wf.getContent() instanceof EditWindow)
//			{
//				EditWindow wnd = (EditWindow)wf.getContent();
//				if (wnd.getCell() == cell)
//				{
//					// figure out full bounds including text
//					Rectangle2D textBounds = obj.getTextBounds(wnd);
//					if (textBounds == null) setChangedInWindow(wnd, bounds); else
//					{
//						Rectangle2D.union(textBounds, bounds, textBounds);
//						setChangedInWindow(wnd, bounds);
//					}
//				}
//			}
//		}
//	}
//
//	/**
//	 * Method to handle the renaming of an ElectricObject.
//	 * @param obj the ElectricObject that was renamed.
//	 * @param oldName the former name of that ElectricObject.
//	 */
//	public void renameObject(ElectricObject obj, Object oldName)
//	{
//		if (obj instanceof Cell)
//		{
//			Cell cell = (Cell)obj;
//			if (cell.isInTechnologyLibrary())
//			{
//				Manipulate.renamedCell((String)oldName, cell.getName());
//			}
//			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//			{
//				WindowFrame wf = it.next();
//				WindowContent content = wf.getContent();
//				if (content.getCell() != cell) continue;
//				content.setWindowTitle();
//			}
//		}
//	}
//
//	/**
//	 * Method to request that an object be redrawn.
//	 * @param obj the ElectricObject to be redrawn.
//	 */
//	public void redrawObject(ElectricObject obj)
//	{
//		Cell cell = null;
//		Rectangle2D bounds = null;
//		if (obj instanceof Geometric)
//		{
//			Geometric geom = (Geometric)obj;
//			cell = geom.getParent();
//		}
//		if (obj instanceof PortInst)
//		{
//			PortInst pi = (PortInst)obj;
//			cell = pi.getNodeInst().getParent();
//		}
//		if (cell != null)
//		{
//			markCellForRedraw(cell, true);
//			for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//			{
//				WindowFrame wf = it.next();
//				if (wf.getContent() instanceof EditWindow)
//				{
//					EditWindow wnd = (EditWindow)wf.getContent();
//					if (wnd.getCell() == cell)
//					{
//						setChangedInWindow(wnd, bounds);
//					}
//				}
//			}
//		}
//	}
//
//	/**
//	 * Method to announce that a Library is about to be saved to disk.
//	 * @param lib the Library that will be written.
//	 */
//	public void writeLibrary(Library lib)
//	{
//	}
//
//	public void startBatch(Tool t, boolean undoRedo)
//	{
//		this.undoRedo = undoRedo;
//
//		// project management tool runs quietly
//		Undo.ChangeBatch batch = Undo.getCurrentBatch();
//		if (batch != null && batch.getTool() == Project.getProjectTool()) this.undoRedo = true;
//	}
//
//	/**
//	 * Daemon Method called when a batch of changes ends.
//	 */
//	public void endBatch()
//	{
//		if (Job.BATCHMODE) return;
//
//		// redraw all windows with Cells that changed
//		for(Iterator<Cell> it = Undo.getChangedCells(); it.hasNext(); )
//		{
//			Cell cell = it.next();
//			markCellForRedraw(cell, true);
//		}
//
//		// update "last designer" field
//		if (!undoRedo)
//		{
//			String userName = System.getProperty("user.name");
//			List<Cell> updateLastDesigner = new ArrayList<Cell>();
//
//			for(Iterator<Cell> it = Undo.getChangedCells(); it.hasNext(); )
//			{
//				Cell cell = it.next();
//				if (!cell.isLinked()) continue;
//
//				// see if the "last designer" should be changed on the cell
//				Variable var = cell.getVar(FRAME_LAST_CHANGED_BY);
//				if (var != null)
//				{
//					String lastDesigner = (String)var.getObject();
//					if (lastDesigner.equals(userName)) continue;
//				}
//
//				// HACK: if cell is checked-in, don't try to modify it
//				int status = Project.getCellStatus(cell);
//				if (status == Project.CHECKEDIN || status == Project.CHECKEDOUTTOOTHERS) continue;
//
//				// must update the "last designer" on this cell
//				updateLastDesigner.add(cell);
//			}
//
//			if (updateLastDesigner.size() > 0)
//			{
//				// change the "last designer" on these cells
//				new SetLastDesigner(userName, updateLastDesigner);
//			}
//		}
//	}
//
//	private static class SetLastDesigner extends Job
//	{
//		private String userName;
//		private List<Cell> updateLastDesigner;
//
//		protected SetLastDesigner(String userName, List<Cell> updateLastDesigner)
//		{
//			super("Set Last Designer", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
//			this.userName = userName;
//			this.updateLastDesigner = updateLastDesigner;
//			startJob();
//		}
//
//		public boolean doIt() throws JobException
//		{
//			for(Cell cell : updateLastDesigner)
//			{
//				cell.newVar(FRAME_LAST_CHANGED_BY, userName);
//			}
//			return true;
//		}
//	}

   /**
	 * Handles database changes of a Job.
	 * @param oldSnapshot database snapshot before Job.
	 * @param newSnapshot database snapshot after Job and constraint propagation.
	 * @param undoRedo true if Job was Undo/Redo job.
	 */
	public void endBatch(Snapshot oldSnapshot, Snapshot newSnapshot, boolean undoRedo) {
		for(Iterator<WindowFrame> wit = WindowFrame.getWindows(); wit.hasNext(); )
		{
			WindowFrame wf = wit.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			Cell winCell = content.getCell();
			if (winCell == null) continue;
			EditWindow wnd = (EditWindow)content;
			if (!winCell.isLinked())
				wnd.setCell(null, null, null);
		}
        if (newSnapshot.environment != IdManager.stdIdManager.getInitialEnvironment()) {
            EditWindow.invokeRenderJob();
        }
//		// Mark cells for redraw
//		HashSet<Cell> marked = new HashSet<Cell>();
//		for (CellId cellId: newSnapshot.getChangedCells(oldSnapshot)) {
//			CellBackup newBackup = newSnapshot.getCell(cellId);
//			CellBackup oldBackup = oldSnapshot.getCell(cellId);
//			ERectangle newBounds = newSnapshot.getCellBounds(cellId);
//			ERectangle oldBounds = oldSnapshot.getCellBounds(cellId);
//			if (newBackup != oldBackup || newBounds != oldBounds) {
//				if (newBackup == null) continue; // What to do with deleted cells ??
//				Cell cell = Cell.inCurrentThread(cellId);
//				if (cell == null) continue; // This might be a desynchronization between GUI thread and delete???
//				markCellForRedrawRecursively(cell, marked);
////				VectorDrawing.cellChanged(cell);
//				EditWindow.forceRedraw(cell);
//			}
//		}
//		for(Iterator<WindowFrame> wit = WindowFrame.getWindows(); wit.hasNext(); )
//		{
//			WindowFrame wf = wit.next();
//			WindowContent content = wf.getContent();
//			if (!(content instanceof EditWindow)) continue;
//			Cell winCell = content.getCell();
//			if (winCell == null) continue;
//			EditWindow wnd = (EditWindow)content;
//			if (!winCell.isLinked()) {
//				wnd.setCell(null, null, null);
//			} else if (marked.contains(winCell)) {
//				wnd.fullRepaint();
//			}
//		}
	}

	/**
	 * Reloading oe renaming libraries has the side affect that any EditWindows
	 * containing cells that were reloaded now point to old, unlinked
	 * cells instead of the new ones. This method checks for this state
	 * and fixes it.
	 * @param idMapper mapping of Library/Cell/Export ids, null if the library was renamed.
	 */
	public static void fixStaleCellReferences(IdMapper idMapper) {
		if (idMapper == null) return;
        AbstractUserInterface ui = Job.getExtendedUserInterface();
        EDatabase database = ui.getDatabase();
        LibId curLibId = ui.getCurrentLibraryId();
        if (curLibId != null && idMapper.get(curLibId) != curLibId)
            ui.setCurrentLibrary(database.getLib(idMapper.get(curLibId)));
		for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
			WindowFrame frame = it.next();
            WindowContent wnd = frame.getContent();
            Cell cell = wnd.getCell();
            if (cell == null) continue;
            if (!cell.isLinked()) {
                CellId cellId = idMapper.get(cell.getId());
                Cell newCell = database.getCell(cellId);
                if (newCell == null) continue;
                wnd.setCell(newCell, VarContext.globalContext, null);
            }
		}
	}

	/************************** TRACKING CHANGES TO CELLS **************************/

	private static Map<EditWindow,Rectangle2D> changedWindowRects = new HashMap<EditWindow,Rectangle2D>();

	/**
	 * Method to tell which area of a window has been changed.
	 * @param wnd the EditWindow in question.
	 * @return the area (in database coordinates) that have been modified and demand redisplay.
	 */
	public static Rectangle2D getChangedInWindow(EditWindow wnd)
	{
		Rectangle2D changedArea = changedWindowRects.get(wnd);
		return changedArea;
	}

	/**
	 * Method to reset the area of a window that has been changed.
	 * Call this after redisplaying that area so that nothing is queued for redraw.
	 * @param wnd the EditWindow in question.
	 */
	public static void clearChangedInWindow(EditWindow wnd)
	{
		changedWindowRects.remove(wnd);
	}

	/**
	 * Method to accumulate the area of a window that has changed and needs redisplay.
	 * @param wnd the EditWindow in question.
	 * @param changedArea the area (in database coordinates) that has changed in the window.
	 */
//	private static void setChangedInWindow(EditWindow wnd, Rectangle2D changedArea)
//	{
////		Rectangle2D lastChanged = changedWindowRects.get(wnd);
////		if (lastChanged == null) changedWindowRects.put(wnd, changedArea); else
////		{
////			Rectangle2D.union(lastChanged, changedArea, lastChanged);
////		}
//	}

//	/**
//	 * Method to recurse flag all windows showing a cell to redraw.
//	 * @param cell the Cell that changed.
//	 * @param cellChanged true if the cell changed and should be marked so.
//	 */
//	public static void markCellForRedraw(Cell cell, boolean cellChanged)
//	{
//		HashSet<Cell> marked = new HashSet<Cell>();
//		markCellForRedrawRecursively(cell, marked);
//		if (cellChanged)
//		{
////			VectorDrawing.cellChanged(cell);
//			EditWindow.forceRedraw(cell);
//			// recurse up the hierarchy so that all windows showing the cell get redrawn
//			for(Iterator<NodeInst> it = cell.getInstancesOf(); it.hasNext(); ) {
//				NodeInst ni = it.next();
//				markCellForRedrawRecursively(ni.getParent(), marked);
//			}
//		}
//
//		for(Iterator<WindowFrame> wit = WindowFrame.getWindows(); wit.hasNext(); )
//		{
//			WindowFrame wf = wit.next();
//			WindowContent content = wf.getContent();
//			if (!(content instanceof EditWindow)) continue;
//			Cell winCell = content.getCell();
//			if (marked.contains(winCell))
//			{
//				EditWindow wnd = (EditWindow)content;
//				wnd.fullRepaint();
//			}
//		}
//	}
//
//	private static void markCellForRedrawRecursively(Cell cell, HashSet<Cell> marked) {
//		if (marked.contains(cell)) return;
//		marked.add(cell);
//		// recurse up the hierarchy so that all windows showing the cell get redrawn
//		for(Iterator<NodeInst> it = cell.getInstancesOf(); it.hasNext(); )
//		{
//			NodeInst ni = it.next();
//			if (ni.isExpanded())
//				markCellForRedrawRecursively(ni.getParent(), marked);
//		}
//	}

	/**
	 * Method called when a technology's parameters change.
	 * All cells that use the technology must be recached.
	 */
	public static void technologyChanged()
	{
		VectorCache.theCache.clearCache();
		EditWindow.clearSubCellCache();
        WindowFrame.updateTechnologyLists();
	}

	/****************************** MISCELLANEOUS FUNCTIONS ******************************/

	/**
	 * Method to return the "current" PrimitivePort per a given pair of arcs, as maintained by the user interface.
	 * @return the "current" PrimitivePort, as maintained by the user interface.
	 */
//	public NodeProto getCurrentNodeProto() { return currentNodeProto; }
    public PrimitivePort getCurrentContactPortProto(ArcProto key1, ArcProto key2)
    {
        Technology tech = key1.getTechnology();
        if (currentTech != tech)
        {
            // need to initialize the data
            uploadCurrentData(tech, tech.getFactoryMenuPalette());
        }
        String key = key1.getName() + "@" + key2.getName();
        PrimitivePort np = currentContactPortProtoMap.get(key);
        if (np != null) return np; // found
        // trying the other combination
        key = key2.getName() + "@" + key1.getName();
        return currentContactPortProtoMap.get(key);
    }

//    /**
//     * Method to return the "current" PrimitivePort per a given PrimitivePort, as maintained by the user interface.
//     * @param p
//     * @return
//     */
//    public PrimitivePort getCurrentContactPortProto(PrimitivePort p)
//    {
//        List<String> list = getArcNamesSorted(p);
//        return currentContactPortProtoMap.get(getKeyFromList(list));
//    }

    public List<PrimitivePort> getPrimitivePortConnectedToArc(ArcProto ap)
    {
        List<PrimitivePort> list = new ArrayList<PrimitivePort>();
        String name = ap.getName();

        // Look if ArcProto name is contained in any key of the contact map. It might not be very efficient.
        for (String key : currentContactPortProtoMap.keySet())
        {
            if (!key.contains(name)) continue; // not a close match
            
            // Using tokenizer as a method to distinguish metal-1 from metal-10
            StringTokenizer t = new StringTokenizer(key, ", @", false);
            while (t.hasMoreTokens())
            {
                String str = t.nextToken();
                if (str.equals(name))
                {
                    PrimitivePort p = currentContactPortProtoMap.get(key);
                    if (p != null)
                        list.add(p);
                    break;
                }
            }
        }
        // not valid for pure layer nodes and well arcs at least
//        if (Job.getDebug() && currentTech.isLayout())
//            assert(!list.isEmpty());
        return list;
    }

//    /**
//	 * Method to set the "current" PrimitivePort per a given pair of arc, as maintained by the user interface.
//	 * @param tech
//	 */
//	public void setCurrentNodeProto(NodeProto np) { currentNodeProto = np; }

    public void setCurrentContactNodeProto(Object obj)
    {
        NodeProto np;
        if (obj instanceof NodeProto)
            np = (NodeProto)obj;
        else if (obj instanceof NodeInst)
            np = ((NodeInst)obj).getProto();
        else if (obj instanceof Xml.PrimitiveNode)
            np = currentTech.findNodeProto(((Xml.PrimitiveNode)obj).name);
        else if (obj instanceof Xml.MenuNodeInst)
            np = currentTech.findNodeProto(((Xml.MenuNodeInst)obj).protoName);
        else
            return; // not the valid object

        if (!(np instanceof PrimitiveNode)) return;
    	updatePrimitiveNodeConnections((PrimitiveNode)np);
    }

    private void updatePrimitiveNodeConnections(PrimitiveNode pn)
    {
        if (pn.isNotUsed()) return;
        if (!pn.getFunction().isContact()) return;
        int numPorts = pn.getNumPorts();
        assert(numPorts == 1); // basic assumption for now.
        for (int j = 0; j < numPorts; j++)
        {
        	PrimitivePort pp = pn.getPort(j);
            List<String> list = getArcNamesSorted(pp);
            for (int i = 1; i < list.size(); i++)
            {
            	for(int k=0; k<i; k++)
            	{
	                String key = list.get(k) + "@" + list.get(i); // @ is not valid for arc names
	                // just 1 combination. getCurrentContactNodeProto would check for both possibilities
	                currentContactPortProtoMap.put(key, pp);
            	}
            }
        }
    }

    /**
     * Method to clean data after switching technologies in the palette.
     */
    public void uploadCurrentData(Technology tech, Xml.MenuPalette menuPalette)
    {
        currentTech = tech;
        equivalentPortProtoMap.clear();
        currentArcProto = null;
        setCurrentArcProto(tech.getArcs().next());

        // rebuild the map of inter-layer contacts
        currentContactPortProtoMap.clear();
        for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
        	updatePrimitiveNodeConnections(it.next());

        // Loading current elements from the XML information if available
//        Xml.MenuPalette menuPalette = Job.getUserInterface().getXmlPalette(tech);
//        int numColumns = menuPalette.numColumns;
        for (int i = 0; i < menuPalette.menuBoxes.size(); i++)
        {
            List<?> menuBoxList = menuPalette.menuBoxes.get(i);
            if (menuBoxList == null || menuBoxList.isEmpty()) continue;
            setCurrentContactNodeProto(menuBoxList.get(0));
        }
    }

    /**
     * Method to set equivalent PortProto in a given technology. Used by wiring tool and set by TechPalette.
     * @param obj
     */
    public void setEquivalentPortProto(Object obj)
    {
        NodeProto np;
        if (obj instanceof NodeProto)
            np = (NodeProto)obj;
        else if (obj instanceof NodeInst)
            np = ((NodeInst)obj).getProto();
        else
            return; // not the valid object
        if (!(np instanceof PrimitiveNode))
            return;

        PrimitiveNode pn = (PrimitiveNode)np;
        if (pn.isNotUsed()) return;

        if (!pn.getFunction().isContact()) return; // only for contacts

        int numPorts = np.getNumPorts();
        assert(numPorts == 1); // basic assumption for now.
        PortProto pp = pn.getPort(0);
        if (pp instanceof PrimitivePort)
        {
            PrimitivePort p = (PrimitivePort)pp;
            List<String> list = getArcNamesSorted(p);
            // adding to list of equivalent ports
            String key = getKeyFromList(list); // lets see what we get
            List<PrimitivePort> l = equivalentPortProtoMap.get(key);
            if (l == null)
            {
                l = new ArrayList<PrimitivePort>();
                equivalentPortProtoMap.put(key, l);
            }
            l.add(p);
        }
    }

    /**
     * Method to provide list of arc names per PrimitivePort. Used in equivalent functions.
     * It doesn't include Generic arc protos.
     * @param p
     * @return Sorted list contained the arc names
     */
    private static List<String> getArcNamesSorted(PrimitivePort p)
    {
        ArcProto[] arcs = p.getConnections();
        List<String> list = new ArrayList<String>(arcs.length);
        // removing the generic arcs
        for (int i = 0; i < arcs.length; i++)
        {
            ArcProto ap = arcs[i];
            if (ap.getTechnology() == Generic.tech()) continue;
            list.add(ap.getName());
        }
//        assert(list.size() > 0); // basic assumption for now
        // Sort list so it could be used for equivalent ports
        Collections.sort(list);
        return list;
    }

    private static String getKeyFromList(List<String> list)
    {
        String key = "";
        for (String s : list)
        {
            key += "@" + s;
        }
        return key;
    }

    /**
     * Method to provide list of equivalent ports based on the arc protos associated to it.
     * @param p the Port to examine for equivalence.
     * @return a List of equivalent ports.
     */
    public List<PrimitivePort> getEquivalentPorts(PrimitivePort p)
    {
        List<String> list = getArcNamesSorted(p);
        return equivalentPortProtoMap.get(getKeyFromList(list));
    }

    /**
	 * Method to return the "current" ArcProto, as maintained by the user interface.
	 * The current ArcProto is highlighted with a bolder red border in the component menu on the left.
	 * @return the "current" ArcProto, as maintained by the user interface.
	 */
	public ArcProto getCurrentArcProto() { return currentArcProto; }

	/**
	 * Method to set the "current" ArcProto, as maintained by the user interface.
	 * The current ArcProto is highlighted with a bolder red border in the component menu on the left.
	 * @param ap the new "current" ArcProto.
	 */
	public void setCurrentArcProto(ArcProto ap)
	{
		currentArcProto = ap;
		WindowFrame wf = WindowFrame.getCurrentWindowFrame(false);
		if (wf != null) wf.getPaletteTab().arcProtoChanged();
	}

	private static AudioClip clickSound = null;
	private static boolean hasSound = true;

	public static void playSound()
	{
		if (!hasSound) return;

		if (clickSound == null)
		{
			// first time: see if there is a sound card
			try
			{
				hasSound = javax.sound.sampled.AudioSystem.getMixerInfo().length > 0;
				if (!hasSound) return;
			}
			catch (Throwable t)
			{
				hasSound = false;
				return;
			}

			// initialize the click sound
			URL url = Resources.getURLResource(TopLevel.class, "Click.wav");
			if (url == null) { hasSound = false;   return; }
			clickSound = Applet.newAudioClip(url);
		}

		// play the sound
		clickSound.play();
	}

	/**
	 * Method to switch libraries and handle technology editing details.
	 */
	public static void setCurrentLibrary(Library lib)
	{
        assert SwingUtilities.isEventDispatchThread();
        Job.getExtendedUserInterface().setCurrentLibrary(lib);
	}

	/****************************** PROJECT PREFERENCES *****************************************/

	/**
	 * Method to get default technique in Tech Palette.
	 * The default is "mocmos".
	 * @return the default technology to use in Tech Palette
	 */
	public static String getDefaultTechnology() { return getDefaultTechnologySetting().getString(); }
	/**
	 * Returns project preference to tell default technique in Tech Palette.
	 * @return project preference to tell default technique in Tech Palette.
	 */
	public static Setting getDefaultTechnologySetting() { return ToolSettings.getDefaultTechnologySetting(); }

	/**
	 * Method to choose the layout Technology to use when schematics are found.
	 * This is important in Spice deck generation (for example) because the Spice primitives may
	 * say "2x3" on them, but a real technology (such as "mocmos") must be found to convert these pure
	 * numbers to real spacings for the deck.
	 * The default is the MOSIS CMOS technology.
	 * @return the Technology to use when schematics are found.
	 */
	public static Technology getSchematicTechnology()
	{
		String t = getSchematicTechnologySetting().getString();
		Technology tech = Technology.findTechnology(t);
		if (tech == null) return Technology.getMocmosTechnology();
		return tech;
	}
	/**
	 * Returns project preference to tell the layout Technology to use when schematics are found.
	 * This is important in Spice deck generation (for example) because the Spice primitives may
	 * say "2x3" on them, but a real technology (such as "mocmos") must be found to convert these pure
	 * numbers to real spacings for the deck.
	 * @return project preference to tell the Technology to use when schematics are found.
	 */
	public static Setting getSchematicTechnologySetting() { return ToolSettings.getSchematicTechnologySetting(); }

	/**
	 * Method to choose the layout Technology to use when schematics are found, by default.
	 * @return the Technology to use when schematics are found, by default.
	 */
	public static Technology getFactorySchematicTechnology()
	{
		String t = (String)getSchematicTechnologySetting().getFactoryValue();
		Technology tech = Technology.findTechnology(t);
		if (tech == null) return Technology.getMocmosTechnology();
		return tech;
	}

	/**
	 * Method to tell whether to include the date and Electric version in output files.
	 * The default is "true".
	 * @return true if the system should include the date and Electric version in output files.
	 */
	public static boolean isIncludeDateAndVersionInOutput() { return getIncludeDateAndVersionInOutputSetting().getBoolean(); }
	/**
	 * Returns project preference to tell whether to include the date and Electric version in output files.
	 * @return project preference to tell whether to include the date and Electric version in output files.
	 */
	public static Setting getIncludeDateAndVersionInOutputSetting() { return ToolSettings.getIncludeDateAndVersionInOutputSetting(); }

	/**
	 * Method to tell whether the process is a PSubstrate process. If true, it will ignore the pwell spacing rule.
	 * The default is "true".
	 * @return true if the process is PSubstrate
	 */
	public static Setting getPSubstrateProcessLayoutTechnologySetting() {return ToolSettings.getPSubstrateProcessLayoutTechnologySetting(); }
	public static boolean isPSubstrateProcessLayoutTechnology() {return getPSubstrateProcessLayoutTechnologySetting().getBoolean();}

	/**
	 * Returns project preference with additional technologies.
	 * @return project preference with additional technologies.
	 */
	public static Setting getSoftTechnologiesSetting() { return ToolSettings.getSoftTechnologiesSetting(); }

	/****************************** ICON GENERATION PREFERENCES ******************************/

	private static Pref cacheIconGenInputSide = Pref.makeIntPref("IconGenInputSide", tool.prefs, 0);
	/**
	 * Method to tell where Input ports should go on generated icons.
	 * @return information about where Input ports should go on generated icons.
	 * 0: left (the default)   1: right   2: top   3: bottom
	 */
	public static int getIconGenInputSide() { return cacheIconGenInputSide.getInt(); }
	/**
	 * Method to set where Input ports should go on generated icons.
	 * @param side information about where Input ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static void setIconGenInputSide(int side) { cacheIconGenInputSide.setInt(side); }
	/**
	 * Method to tell where Input ports should go on generated icons, by default.
	 * @return information about where Input ports should go on generated icons, by default.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static int getFactoryIconGenInputSide() { return cacheIconGenInputSide.getIntFactoryValue(); }

	private static Pref cacheIconGenOutputSide = Pref.makeIntPref("IconGenOutputSide", tool.prefs, 1);
	/**
	 * Method to tell where Output ports should go on generated icons.
	 * @return information about where Output ports should go on generated icons.
	 * 0: left   1: right (the default)   2: top   3: bottom
	 */
	public static int getIconGenOutputSide() { return cacheIconGenOutputSide.getInt(); }
	/**
	 * Method to set where Output ports should go on generated icons.
	 * @param side information about where Output ports should go on generated icons.
	 * 0: left   1: right (the default)   2: top   3: bottom
	 */
	public static void setIconGenOutputSide(int side) { cacheIconGenOutputSide.setInt(side); }
	/**
	 * Method to tell where Output ports should go on generated icons, by default.
	 * @return information about where Output ports should go on generated icons, by default.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static int getFactoryIconGenOutputSide() { return cacheIconGenOutputSide.getIntFactoryValue(); }

	private static Pref cacheIconGenBidirSide = Pref.makeIntPref("IconGenBidirSide", tool.prefs, 2);
	/**
	 * Method to tell where Bidirectional ports should go on generated icons.
	 * @return information about where Bidirectional ports should go on generated icons.
	 * 0: left   1: right   2: top (the default)   3: bottom
	 */
	public static int getIconGenBidirSide() { return cacheIconGenBidirSide.getInt(); }
	/**
	 * Method to set where Bidirectional ports should go on generated icons.
	 * @param side information about where Bidirectional ports should go on generated icons.
	 * 0: left   1: right   2: top (the default)   3: bottom
	 */
	public static void setIconGenBidirSide(int side) { cacheIconGenBidirSide.setInt(side); }
	/**
	 * Method to tell where Bidirectional ports should go on generated icons, by default.
	 * @return information about where Bidirectional ports should go on generated icons, by default.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static int getFactoryIconGenBidirSide() { return cacheIconGenBidirSide.getIntFactoryValue(); }

	private static Pref cacheIconGenPowerSide = Pref.makeIntPref("IconGenPowerSide", tool.prefs, 3);
	/**
	 * Method to tell where Power ports should go on generated icons.
	 * @return information about where Power ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	public static int getIconGenPowerSide() { return cacheIconGenPowerSide.getInt(); }
	/**
	 * Method to set where Power ports should go on generated icons.
	 * @param side information about where Power ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	public static void setIconGenPowerSide(int side) { cacheIconGenPowerSide.setInt(side); }
	/**
	 * Method to tell where Power ports should go on generated icons, by default.
	 * @return information about where Power ports should go on generated icons, by default.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static int getFactoryIconGenPowerSide() { return cacheIconGenPowerSide.getIntFactoryValue(); }

	private static Pref cacheIconGenGroundSide = Pref.makeIntPref("IconGenGroundSide", tool.prefs, 3);
	/**
	 * Method to tell where Ground ports should go on generated icons.
	 * @return information about where Ground ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	public static int getIconGenGroundSide() { return cacheIconGenGroundSide.getInt(); }
	/**
	 * Method to set where Ground ports should go on generated icons.
	 * @param side information about where Ground ports should go on generated icons.
	 * 0: left   1: right   2: top   3: bottom (the default)
	 */
	public static void setIconGenGroundSide(int side) { cacheIconGenGroundSide.setInt(side); }
	/**
	 * Method to tell where Ground ports should go on generated icons, by default.
	 * @return information about where Ground ports should go on generated icons, by default.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static int getFactoryIconGenGroundSide() { return cacheIconGenGroundSide.getIntFactoryValue(); }

	private static Pref cacheIconGenClockSide = Pref.makeIntPref("IconGenClockSide", tool.prefs, 0);
	/**
	 * Method to tell where Clock ports should go on generated icons.
	 * @return information about where Clock ports should go on generated icons.
	 * 0: left (the default)   1: right   2: top   3: bottom
	 */
	public static int getIconGenClockSide() { return cacheIconGenClockSide.getInt(); }
	/**
	 * Method to set where Clock ports should go on generated icons.
	 * @param side information about where Clock ports should go on generated icons.
	 * 0: left (the default)   1: right   2: top   3: bottom
	 */
	public static void setIconGenClockSide(int side) { cacheIconGenClockSide.setInt(side); }
	/**
	 * Method to tell where Clock ports should go on generated icons, by default.
	 * @return information about where Clock ports should go on generated icons, by default.
	 * 0: left   1: right   2: top   3: bottom
	 */
	public static int getFactoryIconGenClockSide() { return cacheIconGenClockSide.getIntFactoryValue(); }

	private static Pref cacheIconGenTopRot = Pref.makeIntPref("IconGenTopRot", tool.prefs, 0);
	/**
	 * Method to tell what angle Top ports should go on generated icons.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @return information about what angle Top ports should go on generated icons.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static int getIconGenTopRot() { return cacheIconGenTopRot.getInt(); }
	/**
	 * Method to set what angle Top ports should go on generated icons.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @param rot information about what angle Top ports should go on generated icons.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static void setIconGenTopRot(int rot) { cacheIconGenTopRot.setInt(rot); }
	/**
	 * Method to tell what angle Top ports should go on generated icons, by default.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @return information about what angle Top ports should go on generated icons, by default.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static int getFactoryIconGenTopRot() { return cacheIconGenTopRot.getIntFactoryValue(); }

	private static Pref cacheIconGenBottomRot = Pref.makeIntPref("IconGenBottomRot", tool.prefs, 0);
	/**
	 * Method to tell what angle Bottom ports should go on generated icons.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @return information about what angle Bottom ports should go on generated icons.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static int getIconGenBottomRot() { return cacheIconGenBottomRot.getInt(); }
	/**
	 * Method to set what angle Bottom ports should go on generated icons.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @param rot information about what angle Bottom ports should go on generated icons.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static void setIconGenBottomRot(int rot) { cacheIconGenBottomRot.setInt(rot); }
	/**
	 * Method to tell what angle Bottom ports should go on generated icons, by default.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @return information about what angle Bottom ports should go on generated icons, by default.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static int getFactoryIconGenBottomRot() { return cacheIconGenBottomRot.getIntFactoryValue(); }

	private static Pref cacheIconGenLeftRot = Pref.makeIntPref("IconGenLeftRot", tool.prefs, 0);
	/**
	 * Method to tell what angle Left ports should go on generated icons.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @return information about what angle Left ports should go on generated icons.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static int getIconGenLeftRot() { return cacheIconGenLeftRot.getInt(); }
	/**
	 * Method to set what angle Left ports should go on generated icons.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @param rot information about what angle Left ports should go on generated icons.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static void setIconGenLeftRot(int rot) { cacheIconGenLeftRot.setInt(rot); }
	/**
	 * Method to tell what angle Left ports should go on generated icons, by default.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @return information about what angle Left ports should go on generated icons, by default.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static int getFactoryIconGenLeftRot() { return cacheIconGenLeftRot.getIntFactoryValue(); }

	private static Pref cacheIconGenRightRot = Pref.makeIntPref("IconGenRightRot", tool.prefs, 0);
	/**
	 * Method to tell what angle Right ports should go on generated icons.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @return information about what angle Right ports should go on generated icons.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static int getIconGenRightRot() { return cacheIconGenRightRot.getInt(); }
	/**
	 * Method to set what angle Right ports should go on generated icons.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @param rot information about what angle Right ports should go on generated icons.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static void setIconGenRightRot(int rot) { cacheIconGenRightRot.setInt(rot); }
	/**
	 * Method to tell what angle Right ports should go on generated icons, by default.
	 * This applies only when ports are placed by "location", not "characteristic".
	 * @return information about what angle Right ports should go on generated icons, by default.
	 * 0: normal   1: rotate 90 degrees   2: rotate 180 degrees   3: rotate 270 degrees
	 */
	public static int getFactoryIconGenRightRot() { return cacheIconGenRightRot.getIntFactoryValue(); }

	/****************************** PORT AND EXPORT PREFERENCES ******************************/

	/**
	 * Method to tell how to display ports.
	 * @return how to display ports.
	 * 0: full port names (the default).
	 * 1: short port names (stopping at the first nonalphabetic character).
	 * 2: ports drawn as crosses.
	 */
	public static int getPortDisplayLevel() { return UserInterfaceMain.getGraphicsPreferences().portDisplayLevel; }
	/**
	 * Method to tell how to display exports.
	 * @return how to display exports.
	 * 0: full export names (the default).
	 * 1: short export names (stopping at the first nonalphabetic character).
	 * 2: exports drawn as crosses.
	 */
	public static int getExportDisplayLevel() { return UserInterfaceMain.getGraphicsPreferences().exportDisplayLevel; }

	private static Pref cacheMoveNodeWithExport = Pref.makeBooleanPref("MoveNodeWithExport", tool.prefs, false);
	/**
	 * Method to tell whether to move a node when its export name moves.
	 * The default is "false", which means that the export text can move independently.
	 * @return true to move a node when its export name moves.
	 */
	public static boolean isMoveNodeWithExport() { return cacheMoveNodeWithExport.getBoolean(); }
	/**
	 * Method to set whether to move a node when its export name moves.
	 * @param on true to move a node when its export name moves.
	 */
	public static void setMoveNodeWithExport(boolean on) { cacheMoveNodeWithExport.setBoolean(on); }
	/**
	 * Method to tell whether to move a node when its export name moves, by default.
	 * @return true to move a node when its export name moves, by default.
	 */
	public static boolean isFactoryMoveNodeWithExport() { return cacheMoveNodeWithExport.getBooleanFactoryValue(); }

	/****************************** SELECTION PREFERENCES ******************************/

	private static Pref cacheEasySelectionOfCellInstances = Pref.makeBooleanPref("EasySelectionOfCellInstances", tool.prefs, true);
	/**
	 * Method to tell whether cell instances are all be easy-to-select.
	 * The default is "true".
	 * @return true if cell instances are all be easy-to-select.
	 */
	public static boolean isEasySelectionOfCellInstances() { return cacheEasySelectionOfCellInstances.getBoolean(); }
	/**
	 * Method to set whether cell instances are all be easy-to-select.
	 * @param on true if cell instances are all to be easy-to-select.
	 */
	public static void setEasySelectionOfCellInstances(boolean on) { cacheEasySelectionOfCellInstances.setBoolean(on); }
	/**
	 * Method to tell whether cell instances are all be easy-to-select by default.
	 * @return true if cell instances are all be easy-to-select by default.
	 */
	public static boolean isFactoryEasySelectionOfCellInstances() { return cacheEasySelectionOfCellInstances.getBooleanFactoryValue(); }

	private static Pref cacheDraggingMustEncloseObjects = Pref.makeBooleanPref("DraggingMustEncloseObjects", tool.prefs, false);
	/**
	 * Method to tell whether dragging a selection rectangle must completely encose objects in order to select them.
	 * The default is "false", which means that the selection rectangle need only touch an object in order to select it.
	 * @return true if dragging a selection rectangle must completely encose objects in order to select them.
	 */
	public static boolean isDraggingMustEncloseObjects() { return cacheDraggingMustEncloseObjects.getBoolean(); }
	/**
	 * Method to set whether dragging a selection rectangle must completely encose objects in order to select them.
	 * @param on true if dragging a selection rectangle must completely encose objects in order to select them.
	 */
	public static void setDraggingMustEncloseObjects(boolean on) { cacheDraggingMustEncloseObjects.setBoolean(on); }
	/**
	 * Method to tell whether dragging a selection rectangle must completely encose objects in order to select them, by default.
	 * @return true if dragging a selection rectangle must completely encose objects in order to select them, by default.
	 */
	public static boolean isFactoryDraggingMustEncloseObjects() { return cacheDraggingMustEncloseObjects.getBooleanFactoryValue(); }

	private static Pref cacheMouseOverHighlighting = Pref.makeBooleanPref("UseMouseOverHighlighting", tool.prefs, true);
	/**
	 * Method to tell whether dragging a selection rectangle must completely encose objects in order to select them.
	 * The default is "false", which means that the selection rectangle need only touch an object in order to select it.
	 * @return true if dragging a selection rectangle must completely encose objects in order to select them.
	 */
	public static boolean isMouseOverHighlightingEnabled() { return cacheMouseOverHighlighting.getBoolean(); }
	/**
	 * Method to set whether dragging a selection rectangle must completely encose objects in order to select them.
	 * @param on true if dragging a selection rectangle must completely encose objects in order to select them.
	 */
	public static void setMouseOverHighlightingEnabled(boolean on) { cacheMouseOverHighlighting.setBoolean(on); }
	/**
	 * Method to tell whether dragging a selection rectangle must completely encose objects in order to select them, by default.
	 * @return true if dragging a selection rectangle must completely encose objects in order to select them, by default.
	 */
	public static boolean isFactoryMouseOverHighlightingEnabled() { return cacheMouseOverHighlighting.getBooleanFactoryValue(); }

	private static Pref cacheHighlightConnectedObjects = Pref.makeBooleanPref("HighlightConnectedObjects", tool.prefs, true);
	/**
	 * Method to set whether to highlight objects connected to the selected object
	 * @return true to highlight objects connected to the selected object, false otherwise
	 */
	public static boolean isHighlightConnectedObjects() { return cacheHighlightConnectedObjects.getBoolean(); }
	/**
	 * Method to get whether to highlight objects connected to the selected object
	 * @param on true to highlight objects connected to the selected object
	 */
	public static void setHighlightConnectedObjects(boolean on) { cacheHighlightConnectedObjects.setBoolean(on); }
	/**
	 * Method to set whether to highlight objects connected to the selected object, by default.
	 * @return true to highlight objects connected to the selected object by default, false otherwise
	 */
	public static boolean isFactoryHighlightConnectedObjects() { return cacheHighlightConnectedObjects.getBooleanFactoryValue(); }

	private static Pref cacheHighlightInvisibleObjects = Pref.makeBooleanPref("HighlightInvisibleObjects", tool.prefs, false);
	/**
	 * Method to set whether to highlight objects whose layers are all invisible.
	 * @return true to highlight objects whose layers are all invisible.
	 */
	public static boolean isHighlightInvisibleObjects() { return cacheHighlightInvisibleObjects.getBoolean(); }
	/**
	 * Method to get whether to highlight objects whose layers are all invisible.
	 * @param on true to highlight objects whose layers are all invisible.
	 */
	public static void setHighlightInvisibleObjects(boolean on) { cacheHighlightInvisibleObjects.setBoolean(on); }
	/**
	 * Method to set whether to highlight objects whose layers are all invisible, by default.
	 * @return true to highlight objects whose layers are all invisible by default.
	 */
	public static boolean isFactoryHighlightInvisibleObjects() { return cacheHighlightInvisibleObjects.getBooleanFactoryValue(); }

	/****************************** GRID AND ALIGNMENT PREFERENCES ******************************/

	private static Pref cacheDefGridXSpacing = Pref.makeDoublePref("DefGridXSpacing", tool.prefs, 1);
	/**
	 * Method to return the default spacing of grid dots in the X direction.
	 * The default is 1.
	 * @return the default spacing of grid dots in the X direction.
	 */
	public static double getDefGridXSpacing() { return cacheDefGridXSpacing.getDouble(); }
	/**
	 * Method to set the default spacing of grid dots in the X direction.
	 * @param dist the default spacing of grid dots in the X direction.
	 */
	public static void setDefGridXSpacing(double dist) { cacheDefGridXSpacing.setDouble(dist); }
	/**
	 * Method to return the default spacing of grid dots in the X direction (factory setting).
	 * @return the default spacing of grid dots in the X direction (factory setting).
	 */
	public static double getFactoryDefGridXSpacing() { return cacheDefGridXSpacing.getDoubleFactoryValue(); }

	private static Pref cacheDefGridYSpacing = Pref.makeDoublePref("DefGridYSpacing", tool.prefs, 1);
	/**
	 * Method to return the default spacing of grid dots in the Y direction.
	 * The default is 1.
	 * @return the default spacing of grid dots in the Y direction.
	 */
	public static double getDefGridYSpacing() { return cacheDefGridYSpacing.getDouble(); }
	/**
	 * Method to set the default spacing of grid dots in the Y direction.
	 * @param dist the default spacing of grid dots in the Y direction.
	 */
	public static void setDefGridYSpacing(double dist) { cacheDefGridYSpacing.setDouble(dist); }
	/**
	 * Method to return the default spacing of grid dots in the Y direction (factory setting).
	 * @return the default spacing of grid dots in the Y direction (factory setting).
	 */
	public static double getFactoryDefGridYSpacing() { return cacheDefGridYSpacing.getDoubleFactoryValue(); }

	private static Pref cacheDefGridXBoldFrequency = Pref.makeIntPref("DefGridXBoldFrequency", tool.prefs, 10);
	/**
	 * Method to return the default frequency of bold grid dots in the X direction.
	 * The default is 10.
	 * @return the default frequency of bold grid dots in the X direction.
	 */
	public static int getDefGridXBoldFrequency() { return cacheDefGridXBoldFrequency.getInt(); }
	/**
	 * Method to set the default frequency of bold grid dots in the X direction.
	 * @param dist the default frequency of bold grid dots in the X direction.
	 */
	public static void setDefGridXBoldFrequency(int dist) { cacheDefGridXBoldFrequency.setInt(dist); }
	/**
	 * Method to return the default frequency of bold grid dots in the X direction (factory setting).
	 * @return the default frequency of bold grid dots in the X direction (factory setting).
	 */
	public static int getFactoryDefGridXBoldFrequency() { return cacheDefGridXBoldFrequency.getIntFactoryValue(); }

	private static Pref cacheDefGridYBoldFrequency = Pref.makeIntPref("DefGridYBoldFrequency", tool.prefs, 10);
	/**
	 * Method to return the default frequency of bold grid dots in the Y direction.
	 * The default is 10.
	 * @return the default frequency of bold grid dots in the Y direction.
	 */
	public static int getDefGridYBoldFrequency() { return cacheDefGridYBoldFrequency.getInt(); }
	/**
	 * Method to set the default frequency of bold grid dots in the Y direction.
	 * @param dist the default frequency of bold grid dots in the Y direction.
	 */
	public static void setDefGridYBoldFrequency(int dist) { cacheDefGridYBoldFrequency.setInt(dist); }
	/**
	 * Method to return the default frequency of bold grid dots in the Y direction (factory setting).
	 * @return the default frequency of bold grid dots in the Y direction (factory setting).
	 */
	public static int getFactoryDefGridYBoldFrequency() { return cacheDefGridYBoldFrequency.getIntFactoryValue(); }

	/**
	 * Method to return the default alignment of objects to the grid.
	 * The default is (1,1), meaning that placement and movement should land on whole grid units.
	 * @return the default alignment of objects to the grid.
	 */
	public static Dimension2D getAlignmentToGrid() {
        return UserInterfaceMain.getEditingPreferences().getAlignmentToGrid();
    }

	/**
	 * Method to return index of the current alignment.
	 * @return the index of the current alignment.
	 */
	public static int getAlignmentToGridIndex() {
        return UserInterfaceMain.getEditingPreferences().getAlignmentToGridIndex();
    }

	/**
	 * Method to return an array of five grid alignment values.
	 * @return an array of five grid alignment values.
	 */
	public static Dimension2D[] getAlignmentToGridVector() {
        return UserInterfaceMain.getEditingPreferences().getAlignmentToGridVector();
	}

    /**
	 * Method to set the default alignment of objects to the grid.
	 * @param dist the array of grid alignment values.
	 * @param current the index in the array that is the current grid alignment.
	 */
	public static void setAlignmentToGridVector(Dimension2D[] dist, int current)
	{
        assert SwingUtilities.isEventDispatchThread();
        UserInterfaceMain.setEditingPreferences(UserInterfaceMain.getEditingPreferences().withAlignment(dist, current));
	}

	private static Pref cacheShowGridAxes = Pref.makeBooleanPref("ShowGridAxes", tool.prefs, false);
	/**
	 * Method to return true if grid axes are shown.
	 * Grid axes are solid lines passing through the origin.
	 * The default is false.
	 * @return true if grid axes are shown.
	 */
	public static boolean isGridAxesShown() { return cacheShowGridAxes.getBoolean(); }
	/**
	 * Method to set if grid axes are shown.
	 * Grid axes are solid lines passing through the origin.
	 * @param s true if grid axes are shown.
	 */
	public static void setGridAxesShown(boolean s) { cacheShowGridAxes.setBoolean(s); }
	/**
	 * Method to return true if grid axes are shown, by default.
	 * Grid axes are solid lines passing through the origin.
	 * @return true if grid axes are shown, by default.
	 */
	public static boolean isFactoryGridAxesShown() { return cacheShowGridAxes.getBooleanFactoryValue(); }

	/****************************** TEXT PREFERENCES ******************************/

	/**
	 * Method to tell whether to draw text of particular text type.
	 * The default is "true".
     * @param textType specified text type
	 * @return true if the system should text of specified type.
	 */
    public static boolean isTextVisibilityOn(TextDescriptor.TextType textType) {
        return UserInterfaceMain.getGraphicsPreferences().isTextVisibilityOn(textType);
    }

	/**
	 * Method to tell whether to draw text that resides on nodes.
	 * This text includes the node name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on nodes.
	 */
	public static boolean isTextVisibilityOnNode() { return isTextVisibilityOn(TextDescriptor.TextType.NODE); }
	/**
	 * Method to tell whether to draw text that resides on arcs.
	 * This text includes the arc name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on arcs.
	 */
	public static boolean isTextVisibilityOnArc() { return isTextVisibilityOn(TextDescriptor.TextType.ARC); }
	/**
	 * Method to tell whether to draw text that resides on ports.
	 * This text includes the port name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on ports.
	 */
	public static boolean isTextVisibilityOnPort() { return isTextVisibilityOn(TextDescriptor.TextType.PORT); }
	/**
	 * Method to tell whether to draw text that resides on exports.
	 * This text includes the export name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on exports.
	 */
	public static boolean isTextVisibilityOnExport() { return isTextVisibilityOn(TextDescriptor.TextType.EXPORT); }
	/**
	 * Method to tell whether to draw text annotation text.
	 * Annotation text is not attached to any node or arc, but appears to move freely about the cell.
	 * In implementation, they are displayable Variables on Generic:invisible-pin nodes.
	 * The default is "true".
	 * @return true if the system should draw annotation text.
	 */
	public static boolean isTextVisibilityOnAnnotation() { return isTextVisibilityOn(TextDescriptor.TextType.ANNOTATION); }
	/**
	 * Method to tell whether to draw the name of on cell instances.
	 * The default is "true".
	 * @return true if the system should draw the name of on cell instances.
	 */
	public static boolean isTextVisibilityOnInstance() { return isTextVisibilityOn(TextDescriptor.TextType.INSTANCE); }
	/**
	 * Method to tell whether to draw text that resides on the cell.
	 * This includes the current cell's parameters or attributes (for example, spice templates).
	 * The default is "true".
	 * @return true if the system should draw text that resides on the cell.
	 */
	public static boolean isTextVisibilityOnCell() { return isTextVisibilityOn(TextDescriptor.TextType.CELL); }

	/**
	 * Method to get the default font to use on the display.
	 * The default is "SansSerif".
	 * @return the default font to use on the display.
	 */
	public static String getDefaultFont() { return UserInterfaceMain.getGraphicsPreferences().defaultFont; }
	/**
	 * Method to get the factory default font to use on the display.
	 * @return the factory default font to use on the display.
	 */
	public static String getFactoryDefaultFont() { return GraphicsPreferences.FACTORY_DEFAULT_FONT; }

	private static Pref cacheDefaultTextCellFont = Pref.makeStringPref("DefaultTextCellFont", tool.prefs, "SansSerif");
	/**
	 * Method to get the default font to use when editing textual cells.
	 * The default is "SansSerif".
	 * @return the default font to use when editing textual cells.
	 */
	public static String getDefaultTextCellFont() { return cacheDefaultTextCellFont.getString(); }
	/**
	 * Method to set the default font to use when editing textual cells.
	 * @param f the default font to use when editing textual cells.
	 */
	public static void setDefaultTextCellFont(String f) { cacheDefaultTextCellFont.setString(f); }
	/**
	 * Method to get the factory default font to use when editing textual cells.
	 * @return the factory default font to use when editing textual cells.
	 */
	public static String getFactoryDefaultTextCellFont() { return cacheDefaultTextCellFont.getStringFactoryValue(); }

	private static Pref cacheDefaultTextCellSize = Pref.makeIntPref("DefaultTextCellSize", tool.prefs, 12);
	/**
	 * Method to tell the size of text in textual cells.
	 * The default is "12".
	 * @return the size of text in textual cells.
	 */
	public static int getDefaultTextCellSize() { return cacheDefaultTextCellSize.getInt(); }
	/**
	 * Method to set the size of text in textual cells.
	 * @param s the size of text in textual cells.
	 */
	public static void setDefaultTextCellSize(int s) { cacheDefaultTextCellSize.setInt(s); }
	/**
	 * Method to tell the size of text in textual cells, by default.
	 * @return the size of text in textual cells, by default.
	 */
	public static int getFactoryDefaultTextCellSize() { return cacheDefaultTextCellSize.getIntFactoryValue(); }

	private static Pref cacheGlobalTextScale = Pref.makeDoublePref("TextGlobalScale", tool.prefs, 1);
	/**
	 * Method to tell the default global text scale factor.
	 * This factor is the initial text scaling factor for new windows and
	 * enlarges or reduces all displayed text.
	 * The default is "1".
	 * @return the default global text scale factor.
	 */
	public static double getGlobalTextScale() { return cacheGlobalTextScale.getDouble(); }
	/**
	 * Method to set the default global text scale factor.
	 * This factor is the initial text scaling factor for new windows and
	 * enlarges or reduces all displayed text.
	 * @param s the default global text scale.
	 */
	public static void setGlobalTextScale(double s) { cacheGlobalTextScale.setDouble(s); }
	/**
	 * Method to tell the factory default global text scale factor.
	 * This factor is the initial text scaling factor for new windows and
	 * enlarges or reduces all displayed text.
	 * @return the factory default global text scale factor.
	 */
	public static double getFactoryGlobalTextScale() { return cacheGlobalTextScale.getDoubleFactoryValue(); }

	private static Pref cacheDefaultTextExternalEditor = Pref.makeStringPref("DefaultTextExternalEditor", tool.prefs, "");
	/**
	 * Method to get the program to invoke when externally editing textual cells.
	 * The default is blank.
	 * @return the program to invoke when externally editing textual cells.
	 */
	public static String getDefaultTextExternalEditor() { return cacheDefaultTextExternalEditor.getString(); }
	/**
	 * Method to set the program to invoke when externally editing textual cells.
	 * @param e the program to invoke when externally editing textual cells.
	 */
	public static void setDefaultTextExternalEditor(String e) { cacheDefaultTextExternalEditor.setString(e); }
	/**
	 * Method to get the program to invoke when externally editing textual cells, by default.
	 * @return the program to invoke when externally editing textual cells, by default.
	 */
	public static String getFactoryDefaultTextExternalEditor() { return cacheDefaultTextExternalEditor.getStringFactoryValue(); }

	/****************************** FRAME PREFERENCES ******************************/

	private static Pref cacheFrameCompanyName = Pref.makeStringPref("FrameCompanyName", tool.prefs, "");
	/**
	 * Method to return the company name to use in schematic frames.
	 * The company information sits in a block in the lower-right corner.
	 * The default is "".
	 * @return the company name to use in schematic frames.
	 */
	public static String getFrameCompanyName() { return cacheFrameCompanyName.getString(); }
	/**
	 * Method to set the company name to use in schematic frames.
	 * The company information sits in a block in the lower-right corner.
	 * @param c the company name to use in schematic frames.
	 */
	public static void setFrameCompanyName(String c) { cacheFrameCompanyName.setString(c); }
	/**
	 * Method to return the company name to use in schematic frames, by default.
	 * The company information sits in a block in the lower-right corner.
	 * @return the company name to use in schematic frames, by default.
	 */
	public static String getFactoryFrameCompanyName() { return cacheFrameCompanyName.getStringFactoryValue(); }

	private static Pref cacheFrameDesignerName = Pref.makeStringPref("FrameDesignerName", tool.prefs, "");
	/**
	 * Method to return the designer name to use in schematic frames.
	 * The designer information sits in a block in the lower-right corner.
	 * The default is "".
	 * @return the designer name to use in schematic frames.
	 */
	public static String getFrameDesignerName() { return cacheFrameDesignerName.getString(); }
	/**
	 * Method to set the designer name to use in schematic frames.
	 * The designer information sits in a block in the lower-right corner.
	 * @param c the designer name to use in schematic frames.
	 */
	public static void setFrameDesignerName(String c) { cacheFrameDesignerName.setString(c); }
	/**
	 * Method to return the designer name to use in schematic frames, by default.
	 * The designer information sits in a block in the lower-right corner.
	 * @return the designer name to use in schematic frames, by default.
	 */
	public static String getFactoryFrameDesignerName() { return cacheFrameDesignerName.getStringFactoryValue(); }

	private static Pref cacheFrameProjectName = Pref.makeStringPref("FrameProjectName", tool.prefs, "");
	/**
	 * Method to return the project name to use in schematic frames.
	 * The project information sits in a block in the lower-right corner.
	 * The default is "".
	 * @return the project name to use in schematic frames.
	 */
	public static String getFrameProjectName() { return cacheFrameProjectName.getString(); }
	/**
	 * Method to set the project name to use in schematic frames.
	 * The project information sits in a block in the lower-right corner.
	 * @param c the project name to use in schematic frames.
	 */
	public static void setFrameProjectName(String c) { cacheFrameProjectName.setString(c); }
	/**
	 * Method to return the project name to use in schematic frames, by default.
	 * The project information sits in a block in the lower-right corner.
	 * @return the project name to use in schematic frames, by default.
	 */
	public static String getFactoryFrameProjectName() { return cacheFrameProjectName.getStringFactoryValue(); }

	/****************************** COLOR PREFERENCES ******************************/

	public enum ColorPrefType {
        /** color of the background on the display. The default is "light gray". */
        BACKGROUND("Background",                    Color.LIGHT_GRAY),
        /** color of the grid on the display. The default is "black". */
        GRID("Grid",                                Color.BLACK),
        /** color of the highlight on the display. The default is "white". */
        HIGHLIGHT("Highlight",                      Color.WHITE),
        NODE_HIGHLIGHT("NodeHighlight",             Color.BLUE),
        /** color of the highlight on the display. The default is "white". */
        MOUSEOVER_HIGHLIGHT("MouseOverHighlight",   new Color(51,255,255)),
        /** color of the port highlight on the display. The default is "yellow". */
		PORT_HIGHLIGHT("PortHighlight",             Color.YELLOW),
        /** color of the text on the display. The default is "black". */
        TEXT("Text",                                Color.BLACK),
        /** color of the instance outlines on the display. The default is "black". */
        INSTANCE("InstanceOutline",                 Color.BLACK),
        /**  color of the border around cells drawn "down in place". The default is "red". */
        DOWNINPLACEBORDER("DownInPlaceBorder",      Color.RED),
        /** color of the waveform window background. The default is "black". */
		WAVE_BACKGROUND("WaveformBackground",       Color.BLACK),
        /** color of the waveform window foreground. This includes lines and text. The default is "white". */
        WAVE_FOREGROUND("WaveformForeground",       Color.WHITE),
        /** color of the traces in waveform windows. Applies only when not a "multistate" display, which uses many colors.
         * The default is "red".
         */
        WAVE_STIMULI("WaveformStimuli",             Color.RED),
        /** color of waveform window traces that have "off" strength. The default is "blue". */
        WAVE_OFF_STRENGTH("WaveformStrengthOff",    Color.BLUE),
        /** color of waveform window traces that have "node" strength. The default is "green". */
		WAVE_NODE_STRENGTH("WaveformStrengthNode",  Color.GREEN),
        /** color of waveform window traces that have "gate" strength. The default is "magenta". */
        WAVE_GATE_STRENGTH("WaveformStrengthGate",  Color.MAGENTA),
        /** color of waveform window traces that have "power" strength. The default is "light gray". */
        WAVE_POWER_STRENGTH("WaveformStrengthPower",Color.LIGHT_GRAY),
        /** color of cross-probe traces from the waveform window that are "low".
         * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
         * The default is "blue".
         */
        WAVE_CROSS_LOW("WaveformCrossProbeLow",     Color.BLUE),
        /** color of cross-probe traces from the waveform window that are "high".
         * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
         * The default is "green".
         */
		WAVE_CROSS_HIGH("WaveformCrossProbeHigh",   Color.GREEN),
        /** color of cross-probe traces from the waveform window that are "undefined".
         * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
         * The default is "black".
         */
        WAVE_CROSS_UNDEF("WaveformCrossProbeX",     Color.BLACK),
        /** color of cross-probe traces from the waveform window that are "floating".
         * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
         * The default is "light_grey".
         */
        WAVE_CROSS_FLOAT("WaveformCrossProbeZ",     Color.LIGHT_GRAY),

        /** color of the cell instance on the 3D display. The default is "gray". */
        INSTANCE_3D("3DColorInstanceCell", Color.GRAY),
        /** color of the highlighted instance on the 3D display. The default is "gray". */
        HIGHLIGHT_3D("3DColorHighlighted", Color.GRAY),
        /** the ambiental color on the 3D display. The default is "gray". */
        AMBIENT_3D("3DColorAmbient", Color.GRAY),
        /** color of the axis X on the 3D display. The default is "red". */
        AXIS_X_3D("3DColorAxisX", Color.RED),
        /** color of the axis X on the 3D display. The default is "blue". */
        AXIS_Y_3D("3DColorAxisY", Color.BLUE),
        /** color of the axis X on the 3D display. The default is "green". */
        AXIS_Z_3D("3DColorAxisZ", Color.GREEN),
        /** color of directional light on the 3D display. The default is "gray". */
        DIRECTIONAL_LIGHT_3D("3DColorDirectionalLight", Color.GRAY);

        private final String prefKey;
        private final Color factoryDefault;

        public String getPrefKey() { return prefKey; }
        public Color getFactoryDefaultColor() { return factoryDefault; }

        private ColorPrefType(String prefKey, Color factoryDefault) {
            this.prefKey = "Color" + prefKey;
            this.factoryDefault = factoryDefault;
        }
    }

	/**
	 * Method to get the color of a given special layer on the display.
	 * BACKGROUND: color of the background on the display. The default is "light gray".
	 * GRID: color of the grid on the display. The default is "black".
	 * HIGHLIGHT: color of the highlight on the display. The default is "white".
	 * MOUSEOVER_HIGHLIGHT: color of the highlight on the display. The default is "white".
	 * PORT_HIGHLIGHT: color of the port highlight on the display. The default is "yellow".
	 * TEXT: color of the text on the display. The default is "black".
	 * INSTANCE: color of the instance outlines on the display. The default is "black".
	 * ARTWORK: default color of the artwork primitives on the display. The default is "black".
	 * DOWNINPLACEBORDER: color of the border around cells drawn "down in place". The default is "red".
	 * WAVE_BACKGROUND: color of the waveform window background. The default is "black".
	 * WAVE_FOREGROUND: color of the waveform window foreground. This includes lines and text. The default is "white".
	 * WAVE_STIMULI: color of the traces in waveform windows. Applies only when not a "multistate" display, which uses many colors.
	 * The default is "red".
	 * WAVE_OFF_STRENGTH: color of waveform window traces that have "off" strength. The default is "blue".
	 * WAVE_NODE_STRENGTH: color of waveform window traces that have "node" strength. The default is "green".
	 * WAVE_GATE_STRENGTH: color of waveform window traces that have "gate" strength. The default is "magenta".
	 * WAVE_POWER_STRENGTH: color of waveform window traces that have "power" strength. The default is "light gray".
	 * WAVE_CROSS_LOW: color of cross-probe traces from the waveform window that are "low". These are lines drawn on the
	 * schematic or layout to correspond with the value in the waveform window. The default is "blue".
	 * WAVE_CROSS_HIGH: color of cross-probe traces from the waveform window that are "high". These are lines drawn on the
	 * schematic or layout to correspond with the value in the waveform window. The default is "green".
	 * WAVE_CROSS_UNDEF: color of cross-probe traces from the waveform window that are "undefined". These are lines drawn on the
	 * schematic or layout to correspond with the value in the waveform window. The default is "black".
	 * WAVE_CROSS_FLOAT: color of cross-probe traces from the waveform window that are "floating". These are lines drawn
	 * on the schematic or layout to correspond with the value in the waveform window. The default is "light gray".
	 * @param pref special layer in question
	 * @return color of the special layer
	 */
	public static int getColor(ColorPrefType pref) {return UserInterfaceMain.getGraphicsPreferences().getColor(pref).getRGB() & GraphicsPreferences.RGB_MASK;}

	/**
	 * Method to set the color of a given special layer
	 * BACKGROUND: color of the background on the display.
	 * GRID: color of the grid on the display.
	 * HIGHLIGHT: color of the highlight on the display.
	 * MOUSEOVER_HIGHLIGHT: color of the highlight on the display.
	 * PORT_HIGHLIGHT: color of the port highlight on the display.
	 * TEXT: color of the text on the display.
	 * INSTANCE: color of the instance outlines on the display.
	 * DOWNINPLACEBORDER: color of the border around cells drawn "down in place". The default is "red".
	 * WAVE_BACKGROUND: color of the waveform window background.
	 * WAVE_FOREGROUND: color of the waveform window foreground. This includes lines and text.
	 * WAVE_STIMULI: color of the traces in waveform windows. Applies only when not a "multistate" display, which uses many colors.
	 * WAVE_OFF_STRENGTH: color of waveform window traces that have "off" strength.
	 * WAVE_NODE_STRENGTH: color of waveform window traces that have "node" strength.
	 * WAVE_GATE_STRENGTH: color of waveform window traces that have "gate" strength.
	 * WAVE_POWER_STRENGTH: color of waveform window traces that have "power" strength.
	 * WAVE_CROSS_LOW: color of cross-probe traces from the waveform window that are "low". These are lines drawn on the
	 * schematic or layout to correspond with the value in the waveform window.
	 * WAVE_CROSS_HIGH: color of cross-probe traces from the waveform window that are "high". These are lines drawn on the
	 * schematic or layout to correspond with the value in the waveform window.
	 * WAVE_CROSS_UNDEF: color of cross-probe traces from the waveform window that are "undefined". These are lines drawn on the
	 * schematic or layout to correspond with the value in the waveform window.
	 * WAVE_CROSS_FLOAT: color of cross-probe traces from the waveform window that are "floating". These are lines drawn
	 * on the schematic or layout to correspond with the value in the waveform window.
	 * @param pref
	 * @param color
	 */
	public static void setColor(ColorPrefType pref, int color)
	{
        UserInterfaceMain.setGraphicsPreferences(UserInterfaceMain.getGraphicsPreferences().withColor(pref, new Color(color)));
	}

	/**
	 * Method to reset to the factory color of a given special layer
	 * BACKGROUND: The default is "light gray".
	 * GRID: The default is "black".
	 * HIGHLIGHT: The default is "white".
	 * MOUSEOVER_HIGHLIGHT: The default is "white".
	 * PORT_HIGHLIGHT: The default is "yellow".
	 * TEXT: The default is "black".
	 * INSTANCE: The default is "black".
	 * DOWNINPLACEBORDER: The default is "red".
	 * WAVE_BACKGROUND: The default is "black".
	 * WAVE_FOREGROUND: The default is "white".
	 * WAVE_STIMULI: The default is "red".
	 * WAVE_OFF_STRENGTH: The default is "blue".
	 * WAVE_NODE_STRENGTH: The default is "green".
	 * WAVE_GATE_STRENGTH: The default is "magenta".
	 * WAVE_POWER_STRENGTH: The default is "light gray".
	 * WAVE_CROSS_LOW: The default is "blue".
	 * WAVE_CROSS_HIGH: The default is "green".
	 * WAVE_CROSS_UNDEF: The default is "black".
	 * WAVE_CROSS_FLOAT: The default is "light gray".
	 * @param pref
	 */
	public static void resetFactoryColor(ColorPrefType pref)
	{
        UserInterfaceMain.setGraphicsPreferences(UserInterfaceMain.getGraphicsPreferences().withColor(pref, pref.getFactoryDefaultColor()));
	}

	/****************************** UNITS PREFERENCES ******************************/

	private static Pref cacheDistanceUnits = Pref.makeIntServerPref("DistanceUnit", tool.prefs, -1);
	/**
	 * Method to get current distance units.
	 * The default is "scalable units".
	 * @return the current distance units.
	 * Returns null to use scalable units.
	 */
	public static TextUtils.UnitScale getDistanceUnits()
	{
		int unitIndex = cacheDistanceUnits.getInt();
		if (unitIndex < 0) return null;
		return TextUtils.UnitScale.findFromIndex(unitIndex);
	}
	/**
	 * Method to set the current distance units.
	 * @param us the current distance units (null to use scalable units).
	 */
	public static void setDistanceUnits(TextUtils.UnitScale us)
	{
		int unitIndex = -1;
		if (us != null) unitIndex = us.getIndex();
		cacheDistanceUnits.setInt(unitIndex);
	}
	/**
	 * Method to get default distance units.
	 * @return the default distance units.
	 * Returns null to use scalable units.
	 */
	public static TextUtils.UnitScale getFactoryDistanceUnits()
	{
		int unitIndex = cacheDistanceUnits.getIntFactoryValue();
		if (unitIndex < 0) return null;
		return TextUtils.UnitScale.findFromIndex(unitIndex);
	}

	private static Pref cacheResistanceUnits = Pref.makeIntPref("ResistanceUnits", tool.prefs, TextUtils.UnitScale.NONE.getIndex());
	/**
	 * Method to get current resistance units.
	 * The default is "ohms".
	 * @return the current resistance units.
	 */
	public static TextUtils.UnitScale getResistanceUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheResistanceUnits.getInt());
	}
	/**
	 * Method to set the current resistance units.
	 * @param us the current resistance units.
	 */
	public static void setResistanceUnits(TextUtils.UnitScale us)
	{
		cacheResistanceUnits.setInt(us.getIndex());
	}
	/**
	 * Method to get default resistance units.
	 * @return the default resistance units.
	 */
	public static TextUtils.UnitScale getFactoryResistanceUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheResistanceUnits.getIntFactoryValue());
	}

	private static Pref cacheCapacitanceUnits = Pref.makeIntPref("CapacitanceUnits", tool.prefs, TextUtils.UnitScale.PICO.getIndex());
	/**
	 * Method to get current capacitance units.
	 * The default is "picofarads".
	 * @return the current capacitance units.
	 */
	public static TextUtils.UnitScale getCapacitanceUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheCapacitanceUnits.getInt());
	}
	/**
	 * Method to set the current capacitance units.
	 * @param us the current capacitance units.
	 */
	public static void setCapacitanceUnits(TextUtils.UnitScale us)
	{
		cacheCapacitanceUnits.setInt(us.getIndex());
	}
	/**
	 * Method to get default capacitance units.
	 * @return the default capacitance units.
	 */
	public static TextUtils.UnitScale getFactoryCapacitanceUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheCapacitanceUnits.getIntFactoryValue());
	}

	private static Pref cacheInductanceUnits = Pref.makeIntPref("InductanceUnits", tool.prefs, TextUtils.UnitScale.NANO.getIndex());
	/**
	 * Method to get current inductance units.
	 * The default is "nanohenrys".
	 * @return the current inductance units.
	 */
	public static TextUtils.UnitScale getInductanceUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheInductanceUnits.getInt());
	}
	/**
	 * Method to set the current inductance units.
	 * @param us the current inductance units.
	 */
	public static void setInductanceUnits(TextUtils.UnitScale us)
	{
		cacheInductanceUnits.setInt(us.getIndex());
	}
	/**
	 * Method to get default inductance units.
	 * @return the default inductance units.
	 */
	public static TextUtils.UnitScale getFactoryInductanceUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheInductanceUnits.getIntFactoryValue());
	}

	private static Pref cacheAmperageUnits = Pref.makeIntPref("AmperageUnits", tool.prefs, TextUtils.UnitScale.MILLI.getIndex());
	/**
	 * Method to get current amperage (current) units.
	 * The default is "milliamps".
	 * @return the current amperage (current) units.
	 */
	public static TextUtils.UnitScale getAmperageUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheAmperageUnits.getInt());
	}
	/**
	 * Method to set the current amperage (current) units.
	 * @param us the current amperage (current) units.
	 */
	public static void setAmperageUnits(TextUtils.UnitScale us)
	{
		cacheAmperageUnits.setInt(us.getIndex());
	}
	/**
	 * Method to get default amperage (current) units.
	 * @return the default amperage (current) units.
	 */
	public static TextUtils.UnitScale getFactoryAmperageUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheAmperageUnits.getIntFactoryValue());
	}

	private static Pref cacheVoltageUnits = Pref.makeIntPref("VoltageUnits", tool.prefs, TextUtils.UnitScale.NONE.getIndex());
	/**
	 * Method to get current voltage units.
	 * The default is "volts".
	 * @return the current voltage) units.
	 */
	public static TextUtils.UnitScale getVoltageUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheVoltageUnits.getInt());
	}
	/**
	 * Method to set the current voltage units.
	 * @param us the current voltage units.
	 */
	public static void setVoltageUnits(TextUtils.UnitScale us)
	{
		cacheVoltageUnits.setInt(us.getIndex());
	}
	/**
	 * Method to get default voltage units.
	 * @return the default voltage) units.
	 */
	public static TextUtils.UnitScale getFactoryVoltageUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheVoltageUnits.getIntFactoryValue());
	}

	private static Pref cacheTimeUnits = Pref.makeIntPref("TimeUnits", tool.prefs, TextUtils.UnitScale.NONE.getIndex());
	/**
	 * Method to get current time units.
	 * The default is "seconds".
	 * @return the current time units.
	 */
	public static TextUtils.UnitScale getTimeUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheTimeUnits.getInt());
	}
	/**
	 * Method to set the current time units.
	 * @param us the current time units.
	 */
	public static void setTimeUnits(TextUtils.UnitScale us)
	{
		cacheTimeUnits.setInt(us.getIndex());
	}
	/**
	 * Method to get default time units.
	 * @return the default time units.
	 */
	public static TextUtils.UnitScale getFactoryTimeUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheTimeUnits.getIntFactoryValue());
	}

	/****************************** MISCELLANEOUS PREFERENCES ******************************/

//	public static final String INITIALWORKINGDIRSETTING_BASEDONOS = "Based on OS";
//	public static final String INITIALWORKINGDIRSETTING_USECURRENTDIR = "Use current directory";
//	public static final String INITIALWORKINGDIRSETTING_USELASTDIR = "Use last used directory";
//	private static final String [] initialWorkingDirectorySettingChoices = {INITIALWORKINGDIRSETTING_BASEDONOS, INITIALWORKINGDIRSETTING_USECURRENTDIR, INITIALWORKINGDIRSETTING_USELASTDIR};
//	private static Pref cacheInitialWorkingDirectorySetting = Pref.makeStringPref("InitialWorkingDirectorySetting", tool.prefs, initialWorkingDirectorySettingChoices[0]);
//
//	/**
//	 * Method to get the way Electric chooses the initial working directory
//	 * @return a string describing the way Electric chooses the initial working directory
//	 */
//	public static String getInitialWorkingDirectorySetting() { return cacheInitialWorkingDirectorySetting.getString(); }
//
//	/**
//	 * Method to set the way Electric chooses the initial working directory
//	 * @param setting one of the String settings from getInitialWorkingDirectorySettings
//	 */
//	public static void setInitialWorkingDirectorySetting(String setting) {
//		for (int i=0; i<initialWorkingDirectorySettingChoices.length; i++) {
//			if ((initialWorkingDirectorySettingChoices[i]).equals(setting)) {
//				cacheInitialWorkingDirectorySetting.setString(setting);
//			}
//		}
//	}
//
//	/**
//	 * Get the choices for the way Electric chooses the initial working directory
//	 * @return an iterator over a list of strings that can be used with setIntialWorkingDirectorySetting()
//	 */
//	public static Iterator<String> getInitialWorkingDirectorySettings() {
//		ArrayList<String> list = new ArrayList<String>();
//		for (int i=0; i<initialWorkingDirectorySettingChoices.length; i++)
//			list.add(initialWorkingDirectorySettingChoices[i]);
//		return list.iterator();
//	}

	private static Pref cacheWorkingDirectory = Pref.makeStringPref("WorkingDirectory", tool.prefs, java.lang.System.getProperty("user.dir"));
	/**
	 * Method to get the path of the current working directory.
	 * The default is the Java "user directory".
	 * @return the path of the current working directory.
	 */
	public static String getWorkingDirectory() { return cacheWorkingDirectory.getString(); }

	/**
	 * Method to set the path of the current working directory.
	 * @param dir the path of the current working directory.
	 */
	public static void setWorkingDirectory(String dir) { cacheWorkingDirectory.setString(dir); }

    /** Root path to the regression directory */
	private static Pref cacheRegressionPath = Pref.makeStringPref("Regression Path", User.getUserTool().prefs, "<set me up>");
	/**
	 * Method to get the path of the regression tests directory.
	 * The default is a String with invalid path.
	 * @return the path of the regression tests directory.
	 */
	public static String getRegressionPath()
	{
		return cacheRegressionPath.getString();
	}

	/**
	 * Method to set the path of the regression tests directory.
	 * @param s the path of the regression tests directory.
	 */
	public static void setRegressionPath(String s)
	{
		cacheRegressionPath.setString(s);
	}

	private static Pref cacheRecentlyOpenedLibraries = Pref.makeStringPref("RecentlyOpenedLibraries", tool.prefs, "");

	/**
	 * Get an array of File paths to recently opened libraries. These should be
	 * libraries the user opened, and not contain reference libraries opened as a
	 * result of a user opening a library.
	 * @return a list of file paths to libraries (without extension - no delib or jelib)
	 */
	public static String [] getRecentlyOpenedLibraries() {
		String libs = cacheRecentlyOpenedLibraries.getString();
		if (libs.equals("")) return new String[0];
		return libs.split("\n");
	}

	/**
	 * Add a file path (no extension) to a list of recently opened libraries.
	 * This should not include reference libraries opened as a side-affect of
	 * opening a library.
	 * @param s the file path to a library
	 */
	public static void addRecentlyOpenedLibrary(String s) {
		int maxLength = 6;
		String [] libs = getRecentlyOpenedLibraries();
		StringBuffer buf = new StringBuffer();
		buf.append(s);
		for (int i=0; i<maxLength && i<libs.length; i++) {
            if (s.equals(libs[i])) continue;
            buf.append("\n");
			buf.append(libs[i]);
		}
		cacheRecentlyOpenedLibraries.setString(buf.toString());
	}

	/**
	 * Clear the list of recently opened libraries.
	 */
	public static void clearRecentlyOpenedLibraries() {
		cacheRecentlyOpenedLibraries.setString("");
	}

	private static Pref cachePromptForIndexWhenDescending = Pref.makeBooleanPref("PromptForIndexWhenDescending", tool.prefs, false);
	/**
	 * Method to tell whether to prompt the user for an array index when descending into arrayed nodes.
	 * When descending into arrayed nodes, the context doesn't know which index is being traversed.
	 * If a simulation window is present, the user is prompted, because the specific index is important.
	 * When Logical Effort information is available, prompting is also necessary.
	 * The default is "false" (do not prompt if other indicators are not present).
	 * @return true to prompt the user for an array index when descending into arrayed nodes.
	 */
	public static boolean isPromptForIndexWhenDescending() { return cachePromptForIndexWhenDescending.getBoolean(); }
	/**
	 * Method to set whether to prompt the user for an array index when descending into arrayed nodes.
	 * When descending into arrayed nodes, the context doesn't know which index is being traversed.
	 * If a simulation window is present, the user is prompted, because the specific index is important.
	 * When Logical Effort information is available, prompting is also necessary.
	 * @param on true to prompt the user for an array index when descending into arrayed nodes.
	 */
	public static void setPromptForIndexWhenDescending(boolean on) { cachePromptForIndexWhenDescending.setBoolean(on); }
	/**
	 * Method to tell whether to prompt the user for an array index when descending into arrayed nodes, by default.
	 * When descending into arrayed nodes, the context doesn't know which index is being traversed.
	 * If a simulation window is present, the user is prompted, because the specific index is important.
	 * When Logical Effort information is available, prompting is also necessary.
	 * @return true to prompt the user for an array index when descending into arrayed nodes by default.
	 */
	public static boolean isFactoryPromptForIndexWhenDescending() { return cachePromptForIndexWhenDescending.getBooleanFactoryValue(); }

	private static Pref cacheBeepAfterLongJobs = Pref.makeBooleanPref("BeepAfterLongJobs", tool.prefs, false);
	/**
	 * Method to tell whether to beep after long jobs.
	 * Any task longer than 1 minute is considered a "long job".
	 * The default is "false".
	 * @return true if the system should beep after long jobs.
	 */
	public static boolean isBeepAfterLongJobs() { return cacheBeepAfterLongJobs.getBoolean(); }
	/**
	 * Method to set whether to beep after long jobs.
	 * Any task longer than 1 minute is considered a "long job".
	 * @param on true if the system should beep after long jobs.
	 */
	public static void setBeepAfterLongJobs(boolean on) { cacheBeepAfterLongJobs.setBoolean(on); }
	/**
	 * Method to tell whether to beep after long jobs, by default.
	 * Any task longer than 1 minute is considered a "long job".
	 * @return true if the system should beep after long jobs, by default.
	 */
	public static boolean isFactoryBeepAfterLongJobs() { return cacheBeepAfterLongJobs.getBooleanFactoryValue(); }

	private static Pref cacheJobVerboseMode = Pref.makeBooleanPref("JobVerboseMode", tool.prefs, false);
	/**
	 * Method to tell if jobs are described in messages window (verbose mode)
	 * The default is "false".
	 * @return true if jobs are described in the messages window.
	 */
	public static boolean isJobVerboseMode() { return cacheJobVerboseMode.getBoolean(); }
	/**
	 * Method to set whether jobs are described in messages window.
	 * @param on true if jobs are described in the messages window.
	 */
	public static void setJobVerboseMode(boolean on) { cacheJobVerboseMode.setBoolean(on); }
	/**
	 * Method to tell if jobs are described in messages window (verbose mode), by default.
	 * @return true if jobs are described in the messages window, by default.
	 */
	public static boolean isFactoryJobVerboseMode() { return cacheJobVerboseMode.getBooleanFactoryValue(); }

	private static Pref cacheRotateLayoutTransistors = Pref.makeBooleanPref("RotateLayoutTransistors", tool.prefs, false);
	/**
	 * Method to tell if layout transistors are rotated 90 degrees in the menu (and initial placement).
	 * The default is "false".
	 * @return true if layout transistors are rotated 90 degrees in the menu (and initial placement).
	 */
	public static boolean isRotateLayoutTransistors() { return cacheRotateLayoutTransistors.getBoolean(); }
	/**
	 * Method to set whether layout transistors are rotated 90 degrees in the menu (and initial placement).
	 * @param on true if layout transistors are rotated 90 degrees in the menu (and initial placement).
	 */
	public static void setRotateLayoutTransistors(boolean on) { cacheRotateLayoutTransistors.setBoolean(on); }
	/**
	 * Method to tell if layout transistors are rotated 90 degrees in the menu (and initial placement), by default.
	 * @return true if layout transistors are rotated 90 degrees in the menu (and initial placement), by default.
	 */
	public static boolean isFactoryRotateLayoutTransistors() { return cacheRotateLayoutTransistors.getBooleanFactoryValue(); }

	private static Pref cacheSideBarOnRight = Pref.makeBooleanPref("SideBarOnRight", tool.prefs, false);
	/**
	 * Method to tell whether to place the side bar on the right by default.
	 * The side bar (with the cell explorer, component menu, and layers) is usually on the left side.
	 * The default is "false" (place on left).
	 * @return true to place the side bar on the right by default.
	 */
	public static boolean isSideBarOnRight() { return cacheSideBarOnRight.getBoolean(); }
	/**
	 * Method to set whether to place the side bar on the right by default.
	 * The side bar (with the cell explorer, component menu, and layers) is usually on the left side.
	 * @param on true to place the side bar on the right by default.
	 */
	public static void setSideBarOnRight(boolean on) { cacheSideBarOnRight.setBoolean(on); }
	/**
	 * Method to tell whether to place the side bar on the right by default (factory setting).
	 * The side bar (with the cell explorer, component menu, and layers) is usually on the left side.
	 * @return true to place the side bar on the right by default (factory setting).
	 */
	public static boolean isFactorySideBarOnRight() { return cacheSideBarOnRight.getBooleanFactoryValue(); }

	private static Pref cacheDefaultWindowTab = Pref.makeIntPref("DefaultWindowTab", tool.prefs, 0);
	/**
	 * Method to tell the default tab to show.
	 * Choices are: 0=components   1=explorer   2=layers.
	 * The default is "0" (components).
	 * @return the default tab to show.
	 */
	public static int getDefaultWindowTab() { return cacheDefaultWindowTab.getInt(); }
	/**
	 * Method to set the default tab to show.
	 * Choices are: 0=components   1=explorer   2=layers.
	 * @param t the default tab to show.
	 */
	public static void setDefaultWindowTab(int t) { cacheDefaultWindowTab.setInt(t); }

	private static Pref cacheDefaultWindowXPos = Pref.makeIntPref("DefaultWindowXPos", tool.prefs, 0);
	private static Pref cacheDefaultWindowYPos = Pref.makeIntPref("DefaultWindowYPos", tool.prefs, 0);
	/**
	 * Method to tell the default position of top-level windows.
	 * The default is "0,0" (top-left).
	 * @return the default position of top-level windows.
	 */
	public static Point getDefaultWindowPos()
	{
		return new Point(cacheDefaultWindowXPos.getInt(), cacheDefaultWindowYPos.getInt());
	}
	/**
	 * Method to set the default position of top-level windows.
	 * @param pt the default position of top-level windows.
	 */
	public static void setDefaultWindowPos(Point pt)
	{
		cacheDefaultWindowXPos.setInt(pt.x);
		cacheDefaultWindowYPos.setInt(pt.y);
	}

	private static Pref cacheDefaultWindowXSize = Pref.makeIntPref("DefaultWindowXSize", tool.prefs, 0);
	private static Pref cacheDefaultWindowYSize = Pref.makeIntPref("DefaultWindowYSize", tool.prefs, 0);
	/**
	 * Method to tell the default size of top-level windows.
	 * The default is null (use screen default).
	 * @return the default position of top-level windows.
	 */
	public static Dimension getDefaultWindowSize()
	{
		if (cacheDefaultWindowXSize.getInt() == 0 || cacheDefaultWindowYSize.getInt() == 0) return null;
		return new Dimension(cacheDefaultWindowXSize.getInt(), cacheDefaultWindowYSize.getInt());
	}
	/**
	 * Method to set the default size of top-level windows.
	 * @param sz the default size of top-level windows.
	 */
	public static void setDefaultWindowSize(Dimension sz)
	{
		cacheDefaultWindowXSize.setInt(sz.width);
		cacheDefaultWindowYSize.setInt(sz.height);
	}

	private static Pref cacheDefaultMessagesXPos = Pref.makeIntPref("DefaultMessagesXPos", tool.prefs, -1);
	private static Pref cacheDefaultMessagesYPos = Pref.makeIntPref("DefaultMessagesYPos", tool.prefs, -1);
	/**
	 * Method to tell the default position of the messages window.
	 * The default is null (use appropriate size for screen).
	 * @return the default position of the messages window.
	 */
	public static Point getDefaultMessagesPos()
	{
		if (cacheDefaultMessagesXPos.getInt() < 0 && cacheDefaultMessagesYPos.getInt() < 0) return null;
		return new Point(cacheDefaultMessagesXPos.getInt(), cacheDefaultMessagesYPos.getInt());
	}
	/**
	 * Method to set the default position of the messages window.
	 * @param pt the default position of the messages window.
	 */
	public static void setDefaultMessagesPos(Point pt)
	{
		cacheDefaultMessagesXPos.setInt(pt.x);
		cacheDefaultMessagesYPos.setInt(pt.y);
	}

	private static Pref cacheDefaultMessagesXSize = Pref.makeIntPref("DefaultMessagesXSize", tool.prefs, 0);
	private static Pref cacheDefaultMessagesYSize = Pref.makeIntPref("DefaultMessagesYSize", tool.prefs, 0);
	/**
	 * Method to tell the default size of the messages window.
	 * The default is null (use screen default).
	 * @return the default position of the messages window.
	 */
	public static Dimension getDefaultMessagesSize()
	{
		if (cacheDefaultMessagesXSize.getInt() == 0 || cacheDefaultMessagesYSize.getInt() == 0) return null;
		return new Dimension(cacheDefaultMessagesXSize.getInt(), cacheDefaultMessagesYSize.getInt());
	}
	/**
	 * Method to set the default size of the messages window.
	 * @param sz the default size of the messages window.
	 */
	public static void setDefaultMessagesSize(Dimension sz)
	{
		cacheDefaultMessagesXSize.setInt(sz.width);
		cacheDefaultMessagesYSize.setInt(sz.height);
	}

	private static Pref cachePlayClickSoundsWhenCreatingArcs = Pref.makeBooleanServerPref("PlayClickSoundsWhenCreatingArcs", tool.prefs, true);
	/**
	 * Method to tell whether to play a "click" sound when an arc is created.
	 * The default is "true".
	 * @return true if the system should play a "click" sound when an arc is created
	 */
	public static boolean isPlayClickSoundsWhenCreatingArcs() { return cachePlayClickSoundsWhenCreatingArcs.getBoolean(); }
	/**
	 * Method to set whether to play a "click" sound when an arc is created
	 * @param on true if the system should play a "click" sound when an arc is created
	 */
	public static void setPlayClickSoundsWhenCreatingArcs(boolean on) { cachePlayClickSoundsWhenCreatingArcs.setBoolean(on); }
	/**
	 * Method to tell whether to play a "click" sound when an arc is created, by default.
	 * @return true if the system should play a "click" sound when an arc is created, by default.
	 */
	public static boolean isFactoryPlayClickSoundsWhenCreatingArcs() { return cachePlayClickSoundsWhenCreatingArcs.getBooleanFactoryValue(); }

	private static Pref cacheShowHierarchicalCursorCoordinates = Pref.makeBooleanPref("ShowHierarchicalCursorCoordinates", tool.prefs, true);
	/**
	 * Method to tell whether to show hierarchical cursor coordinates as they move in the edit window.
	 * Hierarchical coordinates are those in higher-levels of the hierarchy.  They are only displayed when
	 * the user has done a "Down Hierarchy" to descend the hierarchy.  The coordinates are displayed from
	 * the topmost Cell that the user visited.
	 * The default is "true".
	 * @return true to show hierarchical cursor coordinates as they move in the edit window.
	 */
	public static boolean isShowHierarchicalCursorCoordinates() { return cacheShowHierarchicalCursorCoordinates.getBoolean(); }
	/**
	 * Method to set whether to show hierarchical cursor coordinates as they move in the edit window.
	 * Hierarchical coordinates are those in higher-levels of the hierarchy.  They are only displayed when
	 * the user has done a "Down Hierarchy" to descend the hierarchy.  The coordinates are displayed from
	 * the topmost Cell that the user visited.
	 * @param on true to show hierarchical cursor coordinates as they move in the edit window.
	 */
	public static void setShowHierarchicalCursorCoordinates(boolean on) { cacheShowHierarchicalCursorCoordinates.setBoolean(on); }
	/**
	 * Method to tell whether to show hierarchical cursor coordinates as they move in the edit window, by default.
	 * Hierarchical coordinates are those in higher-levels of the hierarchy.  They are only displayed when
	 * the user has done a "Down Hierarchy" to descend the hierarchy.  The coordinates are displayed from
	 * the topmost Cell that the user visited.
	 * @return true to show hierarchical cursor coordinates as they move in the edit window, by default.
	 */
	public static boolean isFactoryShowHierarchicalCursorCoordinates() { return cacheShowHierarchicalCursorCoordinates.getBooleanFactoryValue(); }

	private static Pref cacheDimUpperLevelWhenDownInPlace = Pref.makeBooleanPref("DimUpperLevelWhenDownInPlace", tool.prefs, true);
	/**
	 * Method to tell whether to dim the upper levels of the display when editing down-in-place.
	 * When editing down-in-place, the upper levels are not editable and are dimmed.
	 * This dimming causes slowdown on some systems and so it can be disabled.
	 * The default is "true".
	 * @return true to dim the upper levels of the display when editing down-in-place.
	 */
	public static boolean isDimUpperLevelWhenDownInPlace() { return cacheDimUpperLevelWhenDownInPlace.getBoolean(); }
	/**
	 * Method to set whether to dim the upper levels of the display when editing down-in-place.
	 * When editing down-in-place, the upper levels are not editable and are dimmed.
	 * This dimming causes slowdown on some systems and so it can be disabled.
	 * @param dim true to dim the upper levels of the display when editing down-in-place.
	 */
	public static void setDimUpperLevelWhenDownInPlace(boolean dim) { cacheDimUpperLevelWhenDownInPlace.setBoolean(dim); }
	/**
	 * Method to tell whether to dim the upper levels of the display when editing down-in-place, by default.
	 * When editing down-in-place, the upper levels are not editable and are dimmed.
	 * This dimming causes slowdown on some systems and so it can be disabled.
	 * @return true to dim the upper levels of the display when editing down-in-place, by default.
	 */
	public static boolean isFactoryDimUpperLevelWhenDownInPlace() { return cacheDimUpperLevelWhenDownInPlace.getBooleanFactoryValue(); }

	private static Pref cacheShowCellsInNewWindow = Pref.makeBooleanPref("ShowCellsInNewWindow", tool.prefs, true);
	/**
	 * Method to tell whether to show cells in a new window (or overwrite the current one).
	 * When true, a new window is popped-up for commands that request to see a cell.
	 * The default is "true".
	 * @return true to show cells in a new window (or overwrite the current one).
	 */
	public static boolean isShowCellsInNewWindow() { return cacheShowCellsInNewWindow.getBoolean(); }
	/**
	 * Method to set whether to show cells in a new window (or overwrite the current one).
	 * When true, a new window is popped-up for commands that request to see a cell.
	 * @param dim true to show cells in a new window (or overwrite the current one).
	 */
	public static void setShowCellsInNewWindow(boolean dim) { cacheShowCellsInNewWindow.setBoolean(dim); }
	/**
	 * Method to tell whether to show cells in a new window, by default.
	 * When true, a new window is popped-up for commands that request to see a cell.
	 * @return true to show cells in a new window, by default.
	 */
	public static boolean isFactoryShowCellsInNewWindow() { return cacheShowCellsInNewWindow.getBooleanFactoryValue(); }

	private static Pref cacheErrorHighlightingPulsate = Pref.makeBooleanPref("ErrorHighlightingPulsate", tool.prefs, true);
	/**
	 * Method to tell whether to show error highlights with pulsating outlines.
	 * The default is "true".
	 * @return true to show error highlights with pulsating outlines.
	 */
	public static boolean isErrorHighlightingPulsate() { return cacheErrorHighlightingPulsate.getBoolean(); }
	/**
	 * Method to set whether to show error highlights with pulsating outlines.
	 * @param dim true to show error highlights with pulsating outlines.
	 */
	public static void setErrorHighlightingPulsate(boolean dim) { cacheErrorHighlightingPulsate.setBoolean(dim); }
	/**
	 * Method to tell whether to show error highlights with pulsating outlines, by default.
	 * @return true to show error highlights with pulsating outlines, by default.
	 */
	public static boolean isFactoryErrorHighlightingPulsate() { return cacheErrorHighlightingPulsate.getBooleanFactoryValue(); }

	private static Pref cacheWhichDisplayAlgorithm = Pref.makeIntPref("WhichDisplayAlgorithm", tool.prefs, 1);
	/**
	 * Method to tell which display algorithm to use.
	 * The default is "1" (vector display algorithm).
	 * @return 0 for the pixel display algorithm (oldest);
	 * 1 for the vector display algorithm (old);
	 * 2 for the layer display algorithm (new).
	 */
	public static int getDisplayAlgorithm() { return cacheWhichDisplayAlgorithm.getInt(); }
	/**
	 * Method to set the display algorithm to use.
	 * @param w 0 for the pixel display algorithm (oldest);
	 * 1 for the vector display algorithm (old);
	 * 2 for the layer display algorithm (new).
	 */
	public static void setDisplayAlgorithm(int w) { cacheWhichDisplayAlgorithm.setInt(w); }
	/**
	 * Method to tell which display algorithm to use, by default.
	 * @return 0 for the pixel display algorithm (oldest);
	 * 1 for the vector display algorithm (old);
	 * 2 for the layer display algorithm (new).
	 */
	public static int getFactoryDisplayAlgorithm() { return cacheWhichDisplayAlgorithm.getIntFactoryValue(); }

	private static Pref cacheUseCellGreekingImages = Pref.makeBooleanPref("UseCellGreekingImages", tool.prefs, false);
	/**
	 * Method to tell whether to use small images when greeking cells.
	 * When not using images, then a single blended color is used for greeked cells.
	 * The default is "false".
	 * @return true to use small images when greeking cells.
	 */
	public static boolean isUseCellGreekingImages() { return cacheUseCellGreekingImages.getBoolean(); }
	/**
	 * Method to set whether to use small images when greeking cells.
	 * When not using images, then a single blended color is used for greeked cells.
	 * @param on true to use small images when greeking cells.
	 */
	public static void setUseCellGreekingImages(boolean on) { cacheUseCellGreekingImages.setBoolean(on); }
	/**
	 * Method to tell whether to use small images when greeking cells, by default.
	 * When not using images, then a single blended color is used for greeked cells.
	 * @return true to use small images when greeking cells, by default.
	 */
	public static boolean isFactoryUseCellGreekingImages() { return cacheUseCellGreekingImages.getBooleanFactoryValue(); }

	private static Pref cacheGreekSizeLimit = Pref.makeDoublePref("GreekSizeLimit", tool.prefs, 3);
	/**
	 * Method to tell the smallest object that can be drawn.
	 * Anything smaller than this amount (in screen pixels) is "greeked", or drawn approximately.
	 * Also, any cell whose complete contents (hierarchically to the bottom) are all smaller
	 * than this size will be "greeked".
	 * The default is 3, meaning that any node or arc smaller than 3 pixels will not be
	 * drawn accurately, but will be "greeked".
	 * @return the smallest object that can be drawn.
	 */
	public static double getGreekSizeLimit() { return cacheGreekSizeLimit.getDouble(); }
	/**
	 * Method to set the smallest object that can be drawn.
	 * Anything smaller than this amount (in screen pixels) is "greeked", or drawn approximately.
	 * Also, any cell whose complete contents (hierarchically to the bottom) are all smaller
	 * than this size will be "greeked".
	 * @param l the smallest object that can be drawn.
	 */
	public static void setGreekSizeLimit(double l) { cacheGreekSizeLimit.setDouble(l); }
	/**
	 * Method to tell the smallest object that can be drawn, by default.
	 * Anything smaller than this amount (in screen pixels) is "greeked", or drawn approximately.
	 * Also, any cell whose complete contents (hierarchically to the bottom) are all smaller
	 * than this size will be "greeked".
	 * @return the smallest object that can be drawn, by default.
	 */
	public static double getFactoryGreekSizeLimit() { return cacheGreekSizeLimit.getDoubleFactoryValue(); }

	private static Pref cacheGreekCellSizeLimit = Pref.makeDoublePref("GreekCellSizeLimit", tool.prefs, 0.1);
	/**
	 * Method to tell the ratio of cell size to screen size beyond which no cell greeking happens.
	 * Any cell that fills more than this fraction of the screen will not be greeked.
	 * The default is 0.1, meaning that cells larger than 10% of the screen will not be greeked.
	 * @return the ratio of cell size to screen size beyond which no cell greeking happens.
	 */
	public static double getGreekCellSizeLimit() { return cacheGreekCellSizeLimit.getDouble(); }
	/**
	 * Method to set the ratio of cell size to screen size beyond which no cell greeking happens.
	 * Any cell that fills more than this fraction of the screen will not be greeked.
	 * @param l the ratio of cell size to screen size beyond which no cell greeking happens.
	 */
	public static void setGreekCellSizeLimit(double l) { cacheGreekCellSizeLimit.setDouble(l); }
	/**
	 * Method to tell the ratio of cell size to screen size beyond which no cell greeking happens, by default.
	 * Any cell that fills more than this fraction of the screen will not be greeked.
	 * @return the ratio of cell size to screen size beyond which no cell greeking happens, by default.
	 */
	public static double getFactoryGreekCellSizeLimit() { return cacheGreekCellSizeLimit.getDoubleFactoryValue(); }

	private static Pref cachePatternedScaleLimit = Pref.makeDoublePref("PatternedScaleLimit", tool.prefs, 0.5/*0.1*/);
	/**
	 * Method to tell the scale of EditWindow when use patterned drawing.
	 * Smaller scales use solid drawing.
	 * The default is 0.5, meaning that 2 lamdas per pixel.
	 * @return the scale of EditWindow when use patterned drawing.
	 */
	public static double getPatternedScaleLimit() { return cachePatternedScaleLimit.getDouble(); }
	/**
	 * Method to set the scale of EditWindow when use patterned drawing.
	 * Smaller scales use solid drawing.
	 * @param l the scale of EditWindow when use patterned drawing.
	 */
	public static void setPatternedScaleLimit(double l) { cachePatternedScaleLimit.setDouble(l); }
	/**
	 * Method to tell the scale of EditWindow when use patterned drawing, by default.
	 * Smaller scales use solid drawing.
	 * @return the scale of EditWindow when use patterned drawing, by default.
	 */
	public static double getFactoryPatternedScaleLimit() { return cachePatternedScaleLimit.getDoubleFactoryValue(); }

	private static Pref cacheLegacyComposite = Pref.makeBooleanPref("LegacyComposite", tool.prefs, false);
	/**
	 * Method to tell whether to use lagacy composite in LayerDrawing.
	 * The default is false, meaning that use (overcolor) alpha blending.
	 * @return true to use lagacy composite in LayerDrawing.
	 */
	public static boolean isLegacyComposite() { return cacheLegacyComposite.getBoolean(); }
	/**
	 * Method to set whether to use lagacy composite in LayerDrawing.
	 * @param on true to use lagacy composite in LayerDrawing.
	 */
	public static void setLegacyComposite(boolean on) { cacheLegacyComposite.setBoolean(on); }
	/**
	 * Method to tell whether to use lagacy composite in LayerDrawing, by default.
	 * @return true to use lagacy composite in LayerDrawing, by default.
	 */
	public static boolean isFactoryLegacyComposite() { return cacheLegacyComposite.getBooleanFactoryValue(); }

	private static Pref cacheAlphaBlendingLimit = Pref.makeDoublePref("AlphaBlendingLimit", tool.prefs, 0.6);
	/**
	 * Method to tell the scale of EditWindow when use overcolor in alpha blending color composite.
	 * Smaller scales don't use overcolor.
	 * The default is 0.6, meaning that 1.66 lamdas per pixel.
	 * @return the scale of EditWindow when use overcolor in alpha blending.
	 */
	public static double getAlphaBlendingOvercolorLimit() { return cacheAlphaBlendingLimit.getDouble(); }
	/**
	 * Method to set the scale of EditWindow when use overcolor in alpha blending color composite.
	 * Smaller scales don't use overcolor.
	 * @param l the scale of EditWindow when use overcolor in alpha blending.
	 */
	public static void setAlphaBlendingOvercolorLimit(double l) { cacheAlphaBlendingLimit.setDouble(l); }
	/**
	 * Method to tell the scale of EditWindow when use overcolor in alpha blending color composite, by default.
	 * Smaller scales don't use overcolor.
	 * @return the scale of EditWindow when use overcolor in alpha blending, by default.
	 */
	public static double getFactoryAlphaBlendingOvercolorLimit() { return cacheAlphaBlendingLimit.getDoubleFactoryValue(); }

	private static Pref cacheShowFileSelectionForNetlists = Pref.makeBooleanPref("ShowFileSelectionForNetlists", tool.prefs, true);
	/**
	 * Method to tell whether to display a file selection dialog before writing netlists.
	 * The default is "true".
	 * @return true if the system should display a file selection dialog before writing netlists.
	 */
	public static boolean isShowFileSelectionForNetlists() { return cacheShowFileSelectionForNetlists.getBoolean(); }
	/**
	 * Method to set whether to display a file selection dialog before writing netlists.
	 * @param on true if the system should display a file selection dialog before writing netlists.
	 */
	public static void setShowFileSelectionForNetlists(boolean on) { cacheShowFileSelectionForNetlists.setBoolean(on); }
	/**
	 * Method to tell whether to display a file selection dialog before writing netlists, by default.
	 * @return true if the system should display a file selection dialog before writing netlists, by default.
	 */
	public static boolean isFactoryShowFileSelectionForNetlists() { return cacheShowFileSelectionForNetlists.getBooleanFactoryValue(); }

	private static Pref cachePanningDistance = Pref.makeIntPref("PanningDistance", tool.prefs, 1);
	/**
	 * Method to tell the distance to pan when shifting the screen or rolling the mouse wheel.
	 * The values are: 0=small, 1=medium, 2=large.
	 * The default is 1.
	 * @return the distance to pan when shifting the screen or rolling the mouse wheel.
	 */
	public static int getPanningDistance() { return cachePanningDistance.getInt(); }
	/**
	 * Method to set the distance to pan when shifting the screen or rolling the mouse wheel.
	 * @param d the distance to pan when shifting the screen or rolling the mouse wheel.
	 * The values are: 0=small, 1=medium, 2=large.
	 */
	public static void setPanningDistance(int d) { cachePanningDistance.setInt(d); }
	/**
	 * Method to tell the distance to pan when shifting the screen or rolling the mouse wheel, by default.
	 * The values are: 0=small, 1=medium, 2=large.
	 * @return the distance to pan when shifting the screen or rolling the mouse wheel, by default.
	 */
	public static int getFactoryPanningDistance() { return cachePanningDistance.getIntFactoryValue(); }

	private static Pref cacheDisplayStyle = Pref.makeIntPref(StartupPrefs.DisplayStyleKey, tool.prefs, StartupPrefs.DisplayStyleDef);
	/**
	 * Method to tell the initial display style for Electric.
	 * The values are: 0=OS default, 1=MDI, 2=SDI.
	 * The default is 0.
	 * @return the display style for Electric.
	 */
	public static int getDisplayStyle() { return cacheDisplayStyle.getInt(); }
	/**
	 * Method to set the initial display style for Electric.
	 * @param s the display style for Electric.
	 * The values are: 0=OS default, 1=MDI, 2=SDI.
	 * Changes do not take effect until Electric is restarted.
	 */
	public static void setDisplayStyle(int s) { cacheDisplayStyle.setInt(s); }
	/**
	 * Method to tell the initial display style for Electric, by default.
	 * The values are: 0=OS default, 1=MDI, 2=SDI.
	 * @return the display style for Electric, by default.
	 */
	public static int getFactoryDisplayStyle() { return cacheDisplayStyle.getIntFactoryValue(); }

    private static Pref cacheEnableLog = Pref.makeBooleanServerPref("EnableLog", tool.prefs, true);
	/**
	 * Method to tell if logging into Electric's logfile is enable..
	 * The default is true.
	 * @return true if logging is enable.
	 */
	public static boolean isEnableLog() { return cacheEnableLog.getBoolean(); }
	/**
	 * Method to enable or disable logging in Electric
	 * @param log true if logging is enable.
	 */
	public static void setEnableLog(boolean log) { cacheEnableLog.setBoolean(log); }
	/**
	 * Method to tell if logging into Electric's logfile is enable, by default.
	 * The default is true.
	 * @return true if logging is enable, by default.
	 */
	public static boolean getFactoryEnableLog() { return cacheEnableLog.getBooleanFactoryValue(); }

    private static Pref cacheMultipleLog = Pref.makeBooleanServerPref("MultipleLog", tool.prefs, false);
	/**
	 * Method to tell if multiple logfiles are enable. If yes, an extra suffix will be added and the logfile
     * name will be Electic-*.log. If not, the logfile will be named Electric.log. The default is false.
	 * @return true if multiple logging is enable.
	 */
	public static boolean isMultipleLog() { return cacheMultipleLog.getBoolean(); }
	/**
	 * Method to enable or disable the generation of multiple logfiles.
	 * @param log true if the generation of multiple logfiles is enable.
	 */
	public static void setMultipleLog(boolean log) { cacheMultipleLog.setBoolean(log); }
	/**
	 * Method to tell if multiple logfiles are enable, by default.
	 * The default is false which means Electric.log will will be the filename.
	 * @return true if multiple logging is enable, by default.
	 */
	public static boolean getFactoryMultipleLog() { return cacheMultipleLog.getBooleanFactoryValue(); }

    private static Pref cacheErrorLimit = Pref.makeIntServerPref("ErrorLimit", tool.prefs, 0);
	/**
	 * Method to tell the maximum number of errors to log.
	 * The default is 0, which means that there is no limit.
	 * @return the maximum number of errors to log.
	 */
	public static int getErrorLimit() { return cacheErrorLimit.getInt(); }
	/**
	 * Method to set the maximum number of errors to log.
	 * @param limit the maximum number of errors to log.
	 * A value of zero indicates that there is no limit.
	 */
	public static void setErrorLimit(int limit) { cacheErrorLimit.setInt(limit); }
	/**
	 * Method to tell the maximum number of errors to log, by default.
	 * The value 0 which means that there is no limit.
	 * @return the maximum number of errors to log, by default.
	 */
	public static int getFactoryErrorLimit() { return cacheErrorLimit.getIntFactoryValue(); }

	private static Pref cacheMaxUndoHistory = Pref.makeIntPref(StartupPrefs.MaxUndoHistoryKey, tool.prefs, StartupPrefs.MaxUndoHistoryDef);
	/**
	 * Method to get the maximum number of undos retained in memory
	 */
	public static int getMaxUndoHistory() { return cacheMaxUndoHistory.getInt(); }
	/**
	 * Method to set the maximum number of undos retained in memory
	 */
	public static void setMaxUndoHistory(int n) { cacheMaxUndoHistory.setInt(n); }
	/**
	 * Method to get the maximum number of undos retained in memory, by default.
	 */
	public static int getFactoryMaxUndoHistory() { return cacheMaxUndoHistory.getIntFactoryValue(); }

	private static Pref cacheMemorySize = Pref.makeIntPref(StartupPrefs.MemorySizeKey, tool.prefs, StartupPrefs.MemorySizeDef);
	/**
	 * Method to tell the maximum memory to use for Electric, in megatybes.
	 * The default is 65 megabytes which is not enough for serious work.
	 * @return the maximum memory to use for Electric (in megabytes).
	 */
	public static int getMemorySize() { return cacheMemorySize.getInt(); }
	/**
	 * Method to set the maximum memory to use for Electric.
	 * @param limit maximum memory to use for Electric (in megabytes).
	 */
	public static void setMemorySize(int limit) { cacheMemorySize.setInt(limit); }
	/**
	 * Method to tell the maximum memory to use for Electric (in megabytes), by default.
	 * @return the maximum memory to use for Electric (in megabytes), by default.
	 */
	public static int getFactoryMemorySize() { return cacheMemorySize.getIntFactoryValue(); }

	private static Pref cachePermSize = Pref.makeIntPref(StartupPrefs.PermSizeKey, tool.prefs, StartupPrefs.PermSizeDef);
	/**
	 * Method to tell the maximum permanent space of 2dn GC to use for Electric, in megatybes.
	 * The default is 0. If zero, value is not considered.
	 * @return the maximum memory to use for Electric (in megabytes).
	 */
	public static int getPermSpace() { return cachePermSize.getInt(); }
	/**
	 * Method to set the maximum permanent space of 2dn GC to use for Electric.
	 * @param limit maximum permanent space of 2dn GC to use for Electric (in megabytes).
	 */
	public static void setPermSpace(int limit) { cachePermSize.setInt(limit); }
	/**
	 * Method to tell the maximum permanent space of 2dn GC to use for Electric (in megatybes), by default.
	 * If zero, value is not considered.
	 * @return the maximum memory to use for Electric (in megabytes), by default.
	 */
	public static int getFactoryPermSpace() { return cachePermSize.getIntFactoryValue(); }

	private static Pref cacheUseTwoJVMs = Pref.makeBooleanPref("UseTwoJVMs", tool.prefs, false);
	/**
	 * Method to tell whether to use two JVMs when running Electric.
	 * When using two JVMs, there is a client and a server, in separate memory spaces.
	 * The default is "false".
	 * @return true to use two JVMs when running Electric.
	 */
	public static boolean isUseTwoJVMs() { return cacheUseTwoJVMs.getBoolean(); }
	/**
	 * Method to set whether to use two JVMs when running Electric.
	 * When using two JVMs, there is a client and a server, in separate memory spaces.
	 * @param on true to use two JVMs when running Electric.
	 */
	public static void setUseTwoJVMs(boolean on) { cacheUseTwoJVMs.setBoolean(on); }
	/**
	 * Method to tell whether to use two JVMs when running Electric, by default.
	 * When using two JVMs, there is a client and a server, in separate memory spaces.
	 * @return true to use two JVMs when running Electric, by default.
	 */
	public static boolean isFactoryUseTwoJVMs() { return cacheUseTwoJVMs.getBooleanFactoryValue(); }

	private static Pref cacheUseClientServer = Pref.makeBooleanPref(StartupPrefs.UseClientServerKey, tool.prefs, StartupPrefs.UseClientServerDef);
	/**
	 * Method to tell whether to use a separate client and server for Electric.
	 * The default is "false".
	 * @return true to use a separate client and server for Electric
	 */
	public static boolean isUseClientServer() { return cacheUseClientServer.getBoolean(); }
	/**
	 * Method to set whether to use a separate client and server for Electric
	 * @param on true to use a separate client and server for Electric
	 */
	public static void setUseClientServer(boolean on) { cacheUseClientServer.setBoolean(on); }
	/**
	 * Method to tell whether to use a separate client and server for Electric, by default.
	 * @return true to use a separate client and server for Electric, by default.
	 */
	public static boolean isFactoryUseClientServer() { return cacheUseClientServer.getBooleanFactoryValue(); }

	private static Pref cacheSnapshotLogging = Pref.makeBooleanPref(StartupPrefs.SnapshotLoggingKey, tool.prefs, StartupPrefs.SnapshotLoggingDef);
	/**
	 * Method to tell whether to perform snapshot logging in a temporary file.
	 * The default is "false".
	 * @return true to perform snapshot logging in a temporary file
	 */
	public static boolean isSnapshotLogging() { return cacheSnapshotLogging.getBoolean(); }
	/**
	 * Method to set whether to perform snapshot logging in a temporary file
	 * @param on true to perform snapshot logging iu a temporary file
	 */
	public static void setSnapshotLogging(boolean on) { cacheSnapshotLogging.setBoolean(on); }
	/**
	 * Method to tell whether to perform snapshot logging in a temporary file, by default.
	 * @return true to perform snapshot logging in a temporary file
	 */
	public static boolean isFactorySnapshotLogging() { return cacheSnapshotLogging.getBooleanFactoryValue(); }

	private static Pref cacheAutoTechnologySwitch = Pref.makeBooleanPref("AutoTechnologySwitch", tool.prefs, true);
	/**
	 * Method to tell whether to switch technologies automatically when changing the current Cell.
	 * Switching technologies means that the component menu updates to the new primitive set.
	 * The default is "true".
	 * @return true if the system should switch technologies automatically when changing the current Cell.
	 */
	public static boolean isAutoTechnologySwitch() { return cacheAutoTechnologySwitch.getBoolean(); }
	/**
	 * Method to set whether to switch technologies automatically when changing the current Cell.
	 * Switching technologies means that the component menu updates to the new primitive set.
	 * @param on true if the system should switch technologies automatically when changing the current Cell.
	 */
	public static void setAutoTechnologySwitch(boolean on) { cacheAutoTechnologySwitch.setBoolean(on); }
	/**
	 * Method to tell whether to switch technologies automatically when changing the current Cell, by default.
	 * Switching technologies means that the component menu updates to the new primitive set.
	 * @return true if the system should switch technologies automatically when changing the current Cell, by default.
	 */
	public static boolean isFactoryAutoTechnologySwitch() { return cacheAutoTechnologySwitch.getBooleanFactoryValue(); }

	private static Pref cacheReconstructArcsAndExportsToDeletedCells = Pref.makeBooleanPref("ReconstructArcsToDeletedCells", tool.prefs, true);
	/**
	 * Method to tell whether to reconstruct arcs and exports to deleted cell instances.
	 * When true, deleting a cell instance will leave the connecting arcs in place (terminated with pins)
	 * and will leave existing exports in place (sitting on pins).
	 * The default is "true".
	 * @return true if the system should reconstruct arcs and exports to deleted cell instances.
	 */
	public static boolean isReconstructArcsAndExportsToDeletedCells() { return cacheReconstructArcsAndExportsToDeletedCells.getBoolean(); }
	/**
	 * Method to set whether to reconstruct arcs and exports to deleted cell instances.
	 * When true, deleting a cell instance will leave the connecting arcs in place (terminated with pins)
	 * and will leave existing exports in place (sitting on pins).
	 * @param on true if the system should reconstruct arcs and exports to deleted cell instances.
	 */
	public static void setReconstructArcsAndExportsToDeletedCells(boolean on) { cacheReconstructArcsAndExportsToDeletedCells.setBoolean(on); }
	/**
	 * Method to tell whether to reconstruct arcs and exports to deleted cell instances, by default.
	 * When true, deleting a cell instance will leave the connecting arcs in place (terminated with pins)
	 * and will leave existing exports in place (sitting on pins).
	 * @return true if the system should reconstruct arcs and exports to deleted cell instances, by default.
	 */
	public static boolean isFactoryReconstructArcsAndExportsToDeletedCells() { return cacheReconstructArcsAndExportsToDeletedCells.getBooleanFactoryValue(); }

	private static Pref cacheConvertSchematicLayoutWhenPasting = Pref.makeBooleanPref("ConvertSchematicLayoutWhenPasting", tool.prefs, true);
	/**
	 * Method to tell whether to convert between schematic and layout views when pasting.
	 * If, for example, a schematic cell instance is copied in a schematic cell and then pasted
	 * into a layout cell, this Preference requests that the layout view be pasted instead.
	 * The default is "true".
	 * @return true if the system should convert between schematic and layout views when pasting.
	 */
	public static boolean isConvertSchematicLayoutWhenPasting() { return cacheConvertSchematicLayoutWhenPasting.getBoolean(); }
	/**
	 * Method to set whether to convert between schematic and layout views when pasting.
	 * If, for example, a schematic cell instance is copied in a schematic cell and then pasted
	 * into a layout cell, this Preference requests that the layout view be pasted instead.
	 * @param on true if the system should convert between schematic and layout views when pasting.
	 */
	public static void setConvertSchematicLayoutWhenPasting(boolean on) { cacheConvertSchematicLayoutWhenPasting.setBoolean(on); }
	/**
	 * Method to tell whether to convert between schematic and layout views when pasting, by default
	 * If, for example, a schematic cell instance is copied in a schematic cell and then pasted
	 * into a layout cell, this Preference requests that the layout view be pasted instead.
	 * @return true if the system should convert between schematic and layout views when pasting, by default.
	 */
	public static boolean isFactoryConvertSchematicLayoutWhenPasting() { return cacheConvertSchematicLayoutWhenPasting.getBooleanFactoryValue(); }

	private static Pref cacheCheckCellDates = Pref.makeBooleanPref("CheckCellDates", tool.prefs, false);
	/**
	 * Method to tell whether to check Cell dates when placing instances.
	 * This is not currently implemented.
	 * The default is "false".
	 * @return true if the system should check Cell dates when placing instances.
	 */
	public static boolean isCheckCellDates() { return cacheCheckCellDates.getBoolean(); }
	/**
	 * Method to set whether to check Cell dates when placing instances.
	 * This is not currently implemented.
	 * @param on true if the system should check Cell dates when placing instances.
	 */
	public static void setCheckCellDates(boolean on) { cacheCheckCellDates.setBoolean(on); }
	/**
	 * Method to tell whether to check Cell dates when placing instances, by default.
	 * This is not currently implemented.
	 * @return true if the system should check Cell dates when placing instances, by default.
	 */
	public static boolean isFactoryCheckCellDates() { return cacheCheckCellDates.getBooleanFactoryValue(); }

	private static Pref cacheDisallowModificationLockedPrims = Pref.makeBooleanServerPref("DisallowModificationLockedPrims", tool.prefs, false);
	/**
	 * Method to tell whether locked primitives can be modified.
	 * Locked primitives occur in array-technologies such as FPGA.
	 * The default is "false".
	 * @return true if the locked primitives cannot be modified.
	 */
	public static boolean isDisallowModificationLockedPrims() { return cacheDisallowModificationLockedPrims.getBoolean(); }
	/**
	 * Method to set whether locked primitives can be modified.
	 * Locked primitives occur in array-technologies such as FPGA.
	 * @param on true if locked primitives cannot be modified.
	 */
	public static void setDisallowModificationLockedPrims(boolean on) { cacheDisallowModificationLockedPrims.setBoolean(on); }
	/**
	 * Method to tell whether locked primitives can be modified by default.
	 * Locked primitives occur in array-technologies such as FPGA.
	 * @return true if the locked primitives cannot be modified by default.
	 */
	public static boolean isFactoryDisallowModificationLockedPrims() { return cacheDisallowModificationLockedPrims.getBooleanFactoryValue(); }

	private static Pref cacheDisallowModificationComplexNodes = Pref.makeBooleanServerPref("DisallowModificationComplexNodes", tool.prefs, false);
	/**
	 * Method to tell whether complex nodes can be modified.
	 * Complex nodes are cell instances and advanced primitives (transistors, etc.)
	 * The default is "false" (modifications are NOT disallowed).
	 * @return true if the complex nodes cannot be modified.
	 */
	public static boolean isDisallowModificationComplexNodes() { return cacheDisallowModificationComplexNodes.getBoolean(); }
	/**
	 * Method to set whether complex nodes can be modified.
	 * Complex nodes are cell instances and advanced primitives (transistors, etc.)
	 * @param on true if complex nodes cannot be modified.
	 */
	public static void setDisallowModificationComplexNodes(boolean on) { cacheDisallowModificationComplexNodes.setBoolean(on); }
	/**
	 * Method to tell whether complex nodes can be modified by default.
	 * Complex nodes are cell instances and advanced primitives (transistors, etc.)
	 * @return true if the complex nodes cannot be modified by default.
	 */
	public static boolean isFactoryDisallowModificationComplexNodes() { return cacheDisallowModificationComplexNodes.getBooleanFactoryValue(); }

	private static Pref cacheMoveAfterDuplicate = Pref.makeBooleanPref("MoveAfterDuplicate", tool.prefs, true);
	/**
	 * Method to tell whether to move objects after duplicating them.
	 * The default is "true".
	 * @return true if the system should move objects after duplicating them.
	 */
	public static boolean isMoveAfterDuplicate() { return cacheMoveAfterDuplicate.getBoolean(); }
	/**
	 * Method to set whether to move objects after duplicating them.
	 * @param on true if the system should move objects after duplicating them.
	 */
	public static void setMoveAfterDuplicate(boolean on) { cacheMoveAfterDuplicate.setBoolean(on); }
	/**
	 * Method to tell whether to move objects after duplicating them, by default.
	 * @return true if the system should move objects after duplicating them, by default.
	 */
	public static boolean isFactoryMoveAfterDuplicate() { return cacheMoveAfterDuplicate.getBooleanFactoryValue(); }

	private static Pref cacheDuplicateInPlace = Pref.makeBooleanPref("DuplicateInPlace", tool.prefs, false);
	/**
	 * Method to tell whether to duplicate in place.
	 * The default is "false".
	 * @return true if the system should duplicate in place.
	 */
	public static boolean isDuplicateInPlace() { return cacheDuplicateInPlace.getBoolean(); }
	/**
	 * Method to set whether to duplicate in place.
	 * @param on true if the system should duplicate objects in place.
	 */
	public static void setDuplicateInPlace(boolean on) { cacheDuplicateInPlace.setBoolean(on); }
	/**
	 * Method to tell whether to duplicate in place.
	 * @return true if the system should duplicate objects in place.
	 */
	public static boolean isFactoryDuplicateInPlace() { return cacheDuplicateInPlace.getBooleanFactoryValue(); }

	private static Pref cacheDupCopiesExports = Pref.makeBooleanPref("DupCopiesExports", tool.prefs, false);
	/**
	 * Method to tell whether Duplicate/Paste/Array of NodeInst copies exports.
	 * The default is "false".
	 * @return true if the system copies exports when doing a Duplicate/Paste/Array of a NodeInst.
	 */
	public static boolean isDupCopiesExports() { return cacheDupCopiesExports.getBoolean(); }
	/**
	 * Method to set whether Duplicate/Paste/Array of NodeInst copies exports.
	 * @param on true if the system copies exports when doing a Duplicate/Paste/Array of a NodeInst.
	 */
	public static void setDupCopiesExports(boolean on) { cacheDupCopiesExports.setBoolean(on); }
	/**
	 * Method to tell whether Duplicate/Paste/Array of NodeInst copies exports, by default.
	 * @return true if the system copies exports when doing a Duplicate/Paste/Array of a NodeInst, by default.
	 */
	public static boolean isFactoryDupCopiesExports() { return cacheDupCopiesExports.getBooleanFactoryValue(); }

	private static Pref cacheIncrementRightmostIndex = Pref.makeBooleanPref("IncrementRightmostIndex", tool.prefs, true);
	/**
	 * Method to tell whether auto-incrementing of array indices works from the rightmost index.
	 * This applies only for multimensional arrays.
	 * The default is "true".
	 * @return true if auto-incrementing of array indices works from the rightmost index.
	 */
	public static boolean isIncrementRightmostIndex() { return cacheIncrementRightmostIndex.getBoolean(); }
	/**
	 * Method to set whether auto-incrementing of array indices works from the rightmost index.
	 * This applies only for multimensional arrays.
	 * @param on true if auto-incrementing of array indices works from the rightmost index.
	 */
	public static void setIncrementRightmostIndex(boolean on) { cacheIncrementRightmostIndex.setBoolean(on); }
	/**
	 * Method to tell whether auto-incrementing of array indices works from the rightmost index, by default.
	 * This applies only for multimensional arrays.
	 * @return true if auto-incrementing of array indices works from the rightmost index, by default.
	 */
	public static boolean isFactoryIncrementRightmostIndex() { return cacheIncrementRightmostIndex.getBooleanFactoryValue(); }

	private static Pref cacheExtractCopiesExports = Pref.makeBooleanPref("ExtractCopiesExports", tool.prefs, true);
	/**
	 * Method to tell whether Extract of NodeInst copies exports.
	 * The default is "false".
	 * @return true if the system copies exports when doing an Extract of a NodeInst.
	 */
	public static boolean isExtractCopiesExports() { return cacheExtractCopiesExports.getBoolean(); }
	/**
	 * Method to set whether Extract of NodeInst copies exports.
	 * @param on true if the system copies exports when doing an Extract of a NodeInst.
	 */
	public static void setExtractCopiesExports(boolean on) { cacheExtractCopiesExports.setBoolean(on); }
	/**
	 * Method to tell whether Extract of NodeInst copies exports, by default.
	 * @return true if the system copies exports when doing an Extract of a NodeInst, by default.
	 */
	public static boolean isFactoryExtractCopiesExports() { return cacheExtractCopiesExports.getBooleanFactoryValue(); }

	private static Pref cacheArcsAutoIncremented = Pref.makeBooleanPref("ArcsAutoIncremented", tool.prefs, true);
	/**
	 * Method to tell whether Duplicate/Paste/Array of ArcInsts auto-increments arc names.
	 * The default is "true".
	 * @return true if the system auto-increments arc names when doing a Duplicate/Paste/Array.
	 */
	public static boolean isArcsAutoIncremented() { return cacheArcsAutoIncremented.getBoolean(); }
	/**
	 * Method to set whether Duplicate/Paste/Array of ArcInsts auto-increments arc names.
	 * @param on true if the system auto-increments arc names when doing a Duplicate/Paste/Array.
	 */
	public static void setArcsAutoIncremented(boolean on) { cacheArcsAutoIncremented.setBoolean(on); }
	/**
	 * Method to tell whether Duplicate/Paste/Array of ArcInsts auto-increments arc names, by default.
	 * @return true if the system auto-increments arc names when doing a Duplicate/Paste/Array, by default.
	 */
	public static boolean isFactoryArcsAutoIncremented() { return cacheArcsAutoIncremented.getBooleanFactoryValue(); }

	private static Pref cacheNewNodeRotation = Pref.makeIntPref("NewNodeRotation", tool.prefs, 0);
	/**
	 * Method to return the default rotation of all new nodes.
	 * The default is 0.
	 * @return the default rotation of all new nodes.
	 */
	public static int getNewNodeRotation() { return cacheNewNodeRotation.getInt(); }
	/**
	 * Method to set the default rotation of all new nodes.
	 * @param rot the default rotation of all new nodes.
	 */
	public static void setNewNodeRotation(int rot) { cacheNewNodeRotation.setInt(rot); }

	private static Pref cacheNewNodeMirrorX = Pref.makeBooleanPref("NewNodeMirrorX", tool.prefs, false);
	/**
	 * Method to tell whether new nodes are mirrored in X.
	 * The default is "false".
	 * @return true if new nodes are mirrored in X.
	 */
	public static boolean isNewNodeMirrorX() { return cacheNewNodeMirrorX.getBoolean(); }
	/**
	 * Method to set whether new nodes are mirrored in X.
	 * @param on true if new nodes are mirrored in X.
	 */
	public static void setNewNodeMirrorX(boolean on) { cacheNewNodeMirrorX.setBoolean(on); }

	private static Pref cacheWaveformDigitalPanelHeight = Pref.makeIntPref("WaveformDigitalPanelHeight", tool.prefs, 25);
	/**
	 * Method to tell the height of digital waveform panels.
	 * The default is "25".
	 * @return the height of digital waveform panels.
	 */
	public static int getWaveformDigitalPanelHeight() { return cacheWaveformDigitalPanelHeight.getInt(); }
	/**
	 * Method to set the height of digital waveform panels.
	 * @param h the height of digital waveform panels.
	 */
	public static void setWaveformDigitalPanelHeight(int h) { cacheWaveformDigitalPanelHeight.setInt(h); }

	private static Pref cacheWaveformAnalogPanelHeight = Pref.makeIntPref("WaveformAnalogPanelHeight", tool.prefs, 75);
	/**
	 * Method to tell the height of analog waveform panels.
	 * The default is "75".
	 * @return the height of analog waveform panels.
	 */
	public static int getWaveformAnalogPanelHeight() { return cacheWaveformAnalogPanelHeight.getInt(); }
	/**
	 * Method to set the height of analog waveform panels.
	 * @param h the height of analog waveform panels.
	 */
	public static void setWaveformAnalogPanelHeight(int h) { cacheWaveformAnalogPanelHeight.setInt(h); }
}
