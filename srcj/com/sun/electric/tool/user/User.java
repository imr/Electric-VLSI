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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.IdMapper;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.VectorCache;
import com.sun.electric.tool.user.ui.WindowContent;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.projectSettings.ProjSettings;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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
	private NodeProto currentNodeProto = null;
	private boolean undoRedo;

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

        if (Job.getRunMode() != Job.Mode.CLIENT)
            Clipboard.clear(); // To initialize Clibpoard Cell
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
//        oldD.computeBounds(ni, oldBounds);
//		Rectangle2D.Double newBounds = new Rectangle2D.Double();
//        ni.getD().computeBounds(ni, newBounds);	// TODO Why can't we use "ni.getBounds()" ?
//		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//		{
//			WindowFrame wf = it.next();
//			if (wf.getContent() instanceof EditWindow)
//			{
//				EditWindow wnd = (EditWindow)wf.getContent();
//				if (wnd.getCell() == cell)
//				{
//					// TODO figure out way to find text bounds on the OLD object
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
//     * @param oD the old contents of the ArcInst.
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
//					// TODO figure out way to find text bounds on the OLD object
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
//        PortInst oldPi = ((Cell)pp.getParent()).getPortInst(oD.originalNodeId, oD.originalPortId);
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
//        redrawObject(cell);
//        CellName oldCellName = oD.cellName;
//        if (cell.getCellName() == oldCellName) return;
//        if (cell.isInTechnologyLibrary()) {
//            Manipulate.renamedCell(oldCellName.getName(), cell.getName());
//        }
//        for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
//            WindowFrame wf = it.next();
//            WindowContent content = wf.getContent();
//            if (content.getCell() != cell) continue;
//            content.setWindowTitle();
//        }
//    }
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
        // Mark cells for redraw
        HashSet<Cell> marked = new HashSet<Cell>();
        for (CellId cellId: newSnapshot.getChangedCells(oldSnapshot)) {
            CellBackup newBackup = newSnapshot.getCell(cellId);
            CellBackup oldBackup = oldSnapshot.getCell(cellId);
            ERectangle newBounds = newSnapshot.getCellBounds(cellId);
            ERectangle oldBounds = oldSnapshot.getCellBounds(cellId);
            if (newBackup != oldBackup || newBounds != oldBounds) {
				if (newBackup == null) continue; // What to do with deleted cells ??
                Cell cell = Cell.inCurrentThread(cellId);
                markCellForRedrawRecursively(cell, marked);
//                VectorDrawing.cellChanged(cell);
                EditWindow.forceRedraw(cell);
            }
        }
		for(Iterator<WindowFrame> wit = WindowFrame.getWindows(); wit.hasNext(); )
		{
			WindowFrame wf = wit.next();
            WindowContent content = wf.getContent();
            if (!(content instanceof EditWindow)) continue;
            Cell winCell = content.getCell();
            if (winCell == null) continue;
            EditWindow wnd = (EditWindow)content;
            if (!winCell.isLinked()) {
                wnd.setCell(null, null, null);
            } else if (marked.contains(winCell)) {
                wnd.repaintContents(null, false);
            }
        }
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
        for (Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); ) {
            WindowFrame frame = it.next();
            if (frame.getContent() instanceof EditWindow) {
                EditWindow wnd = (EditWindow)frame.getContent();
                Cell cell = wnd.getCell();
                if (cell == null) continue;
                if (!cell.isLinked()) {
                    CellId cellId = idMapper.get(cell.getId());
                    Cell newCell = EDatabase.clientDatabase().getCell(cellId);
                    if (newCell == null) continue;
                    wnd.setCell(newCell, VarContext.globalContext, null);
                }
            }
        }
    }

	/************************** TRACKING CHANGES TO CELLS **************************/

	private static HashMap<EditWindow,Rectangle2D> changedWindowRects = new HashMap<EditWindow,Rectangle2D>();

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
	private static void setChangedInWindow(EditWindow wnd, Rectangle2D changedArea)
	{
//		Rectangle2D lastChanged = changedWindowRects.get(wnd);
//		if (lastChanged == null) changedWindowRects.put(wnd, changedArea); else
//		{
//			Rectangle2D.union(lastChanged, changedArea, lastChanged);
//		}
	}

	/**
	 * Method to recurse flag all windows showing a cell to redraw.
	 * @param cell the Cell that changed.
	 * @param cellChanged true if the cell changed and should be marked so.
	 */
	public static void markCellForRedraw(Cell cell, boolean cellChanged)
	{
        HashSet<Cell> marked = new HashSet<Cell>();
        markCellForRedrawRecursively(cell, marked);
		if (cellChanged)
		{
//			VectorDrawing.cellChanged(cell);
			EditWindow.forceRedraw(cell);
            // recurse up the hierarchy so that all windows showing the cell get redrawn
            for(Iterator<NodeInst> it = cell.getInstancesOf(); it.hasNext(); ) {
                NodeInst ni = it.next();
                markCellForRedrawRecursively(ni.getParent(), marked);
            }
		}

		for(Iterator<WindowFrame> wit = WindowFrame.getWindows(); wit.hasNext(); )
		{
			WindowFrame wf = wit.next();
			WindowContent content = wf.getContent();
			if (!(content instanceof EditWindow)) continue;
			Cell winCell = content.getCell();
			if (marked.contains(winCell))
			{
				EditWindow wnd = (EditWindow)content;
				wnd.repaintContents(null, false);
			}
		}
	}

    private static void markCellForRedrawRecursively(Cell cell, HashSet<Cell> marked) {
        if (marked.contains(cell)) return;
        marked.add(cell);
		// recurse up the hierarchy so that all windows showing the cell get redrawn
		for(Iterator<NodeInst> it = cell.getInstancesOf(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.isExpanded())
				markCellForRedrawRecursively(ni.getParent(), marked);
		}
    }

	/**
	 * Method called when a technology's parameters change.
	 * All cells that use the technology must be recached.
	 */
	public static void technologyChanged()
	{
        VectorCache.theCache.clearCache();
        EditWindow.clearSubCellCache();
	}

	/**
	 * Method called when visible layers have changed.
	 * Removes all "greeked images" from cached cells.
	 */
	public static void layerVisibilityChanged(boolean onlyText) {
		if (!onlyText)
			VectorCache.theCache.clearFadeImages();
        EditWindow.clearSubCellCache();
		EditWindow.repaintAllContents();
    }

	/****************************** MISCELLANEOUS FUNCTIONS ******************************/

	/**
	 * Method to return the "current" NodeProto, as maintained by the user interface.
	 * @return the "current" NodeProto, as maintained by the user interface.
	 */
	public NodeProto getCurrentNodeProto() { return currentNodeProto; }

	/**
	 * Method to set the "current" NodeProto, as maintained by the user interface.
	 * @param np the new "current" NodeProto.
	 */
	public void setCurrentNodeProto(NodeProto np) { currentNodeProto = np; }

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
		if (!isPlayClickSoundsWhenCreatingArcs()) return;

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

	/****************************** ICON GENERATION PREFERENCES ******************************/

	private static Pref cacheIconGenDrawLeads = Pref.makeBooleanPref("IconGenDrawLeads", tool.prefs, true);
	/**
	 * Method to tell whether generated icons should have leads drawn.
	 * The default is "true".
	 * @return true if generated icons should have leads drawn.
	 */
	public static boolean isIconGenDrawLeads() { return cacheIconGenDrawLeads.getBoolean(); }
	/**
	 * Method to set whether generated icons should have leads drawn.
	 * @param on true if generated icons should have leads drawn.
	 */
	public static void setIconGenDrawLeads(boolean on) { cacheIconGenDrawLeads.setBoolean(on); }

	private static Pref cacheIconGenDrawBody = Pref.makeBooleanPref("IconGenDrawBody", tool.prefs, true);
	/**
	 * Method to tell whether generated icons should have a body drawn.
	 * The body is just a rectangle.
	 * The default is "true".
	 * @return true if generated icons should have a body drawn.
	 */
	public static boolean isIconGenDrawBody() { return cacheIconGenDrawBody.getBoolean(); }
	/**
	 * Method to set whether generated icons should have a body drawn.
	 * The body is just a rectangle.
	 * @param on true if generated icons should have a body drawn.
	 */
	public static void setIconGenDrawBody(boolean on) { cacheIconGenDrawBody.setBoolean(on); }

	private static Pref cacheIconGenReverseExportOrder = Pref.makeBooleanPref("IconGenReverseExportOrder", tool.prefs, false);
	/**
	 * Method to tell whether generated icons should reverse the order of exports.
	 * Normally, exports are drawn top-to-bottom alphabetically.
	 * The default is "false".
	 * @return true if generated icons should reverse the order of exports.
	 */
	public static boolean isIconGenReverseExportOrder() { return cacheIconGenReverseExportOrder.getBoolean(); }
	/**
	 * Method to set whether generated icons should reverse the order of exports.
	 * Normally, exports are drawn top-to-bottom alphabetically.
	 * @param on true if generated icons should reverse the order of exports.
	 */
	public static void setIconGenReverseExportOrder(boolean on) { cacheIconGenReverseExportOrder.setBoolean(on); }

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

	private static Pref cacheIconGenExportLocation = Pref.makeIntPref("IconGenExportLocation", tool.prefs, 1);
	/**
	 * Method to tell where exports should appear along the leads in generated icons.
	 * @return information about where exports should appear along the leads in generated icons.
	 * 0: on the body   1: (the default) at the end of the lead   2: in the middle of the lead
	 */
	public static int getIconGenExportLocation() { return cacheIconGenExportLocation.getInt(); }
	/**
	 * Method to set how exports should appear along the leads in generated icons.
	 * @param loc information about where exports should appear along the leads in generated icons.
	 * 0: on the body   1: at the end of the lead (the default)   2: in the middle of the lead
	 */
	public static void setIconGenExportLocation(int loc) { cacheIconGenExportLocation.setInt(loc); }

	private static Pref cacheIconGenExportStyle = Pref.makeIntPref("IconGenExportStyle", tool.prefs, 0);
	/**
	 * Method to tell how the text should appear in generated icons.
	 * @return information about how the text should appear in generated icons.
	 * 0: (the default) centered at the export location
	 * 1: pointing inward from the export location
	 * 2: pointing outward from the export location
	 */
	public static int getIconGenExportStyle() { return cacheIconGenExportStyle.getInt(); }
	/**
	 * Method to set how the text should appear in generated icons.
	 * @param style information about how the text should appear in generated icons.
	 * 0: centered at the export location
	 * 1: pointing inward from the export location
	 * 2: pointing outward from the export location
	 */
	public static void setIconGenExportStyle(int style) { cacheIconGenExportStyle.setInt(style); }

	private static Pref cacheIconGenExportTech = Pref.makeIntPref("IconGenExportTech", tool.prefs, 0);
	/**
	 * Method to tell how exports should be constructed in generated icons.
	 * @return information about how exports should be constructed in generated icons.
	 * 0: (the default) use Generic:Universal-Pins for non-bus exports (can connect to ANYTHING).
	 * 1: use Schematic:Bus-Pins for exports (can connect to schematic busses or wires).
	 */
	public static int getIconGenExportTech() { return cacheIconGenExportTech.getInt(); }
	/**
	 * Method to set how exports should be constructed in generated icons.
	 * @param t information about how exports should be constructed in generated icons.
	 * 0: use Generic:Universal-Pins for non-bus exports (can connect to ANYTHING).
	 * 1: use Schematic:Bus-Pins for exports (can connect to schematic busses or wires).
	 */
	public static void setIconGenExportTech(int t) { cacheIconGenExportTech.setInt(t); }

	private static Pref cacheIconGenInstanceLocation = Pref.makeIntPref("IconGenInstanceLocation", tool.prefs, 0);
	/**
	 * Method to tell where to place an instance of the generated icons in the original schematic.
	 * @return information about where to place an instance of the generated icons in the original schematic
	 * 0: (the default) in the upper-right corner.
	 * 1: in the upper-left corner.
	 * 2: in the lower-right corner.
	 * 3: in the lower-left corner.
	 */
	public static int getIconGenInstanceLocation() { return cacheIconGenInstanceLocation.getInt(); }
	/**
	 * Method to set where to place an instance of the generated icons in the original schematic.
	 * @param loc information about where to place an instance of the generated icons in the original schematic.
	 * 0: in the upper-right corner.
	 * 1: in the upper-left corner.
	 * 2: in the lower-right corner.
	 * 3: in the lower-left corner.
	 */
	public static void setIconGenInstanceLocation(int loc) { cacheIconGenInstanceLocation.setInt(loc); }

	private static Pref cacheIconGenLeadLength = Pref.makeDoublePref("IconGenLeadLength", tool.prefs, 2.0f);
	/**
	 * Method to tell how long to make leads in generated icons.
	 * @return information about how long to make leads in generated icons (the default is 2).
	 */
	public static double getIconGenLeadLength() { return cacheIconGenLeadLength.getDouble(); }
	/**
	 * Method to set how long to make leads in generated icons.
	 * @param len how long to make leads in generated icons.
	 */
	public static void setIconGenLeadLength(double len) { cacheIconGenLeadLength.setDouble(len); }

	private static Pref cacheIconGenLeadSpacing = Pref.makeDoublePref("IconGenLeadSpacing", tool.prefs, 2.0f);
	/**
	 * Method to tell how far apart to space leads in generated icons.
	 * @return information about how far apart to space leads in generated icons (the default is 2).
	 */
	public static double getIconGenLeadSpacing() { return cacheIconGenLeadSpacing.getDouble(); }
	/**
	 * Method to set how far apart to space leads in generated icons.
	 * @param dist how far apart to space leads in generated icons.
	 */
	public static void setIconGenLeadSpacing(double dist) { cacheIconGenLeadSpacing.setDouble(dist); }

	/****************************** PORT AND EXPORT PREFERENCES ******************************/

	private static Pref cachePortDisplayLevel = Pref.makeIntPref("PortDisplayLevel", tool.prefs, 0);
	/**
	 * Method to tell how to display ports.
	 * @return how to display ports.
	 * 0: full port names (the default).
	 * 1: short port names (stopping at the first nonalphabetic character).
	 * 2: ports drawn as crosses.
	 */
	public static int getPortDisplayLevel() { return cachePortDisplayLevel.getInt(); }
	/**
	 * Method to set how to display ports.
	 * @param level how to display ports.
	 * 0: full port names (the default).
	 * 1: short port names (stopping at the first nonalphabetic character).
	 * 2: ports drawn as crosses.
	 */
	public static void setPortDisplayLevels(int level) { cachePortDisplayLevel.setInt(level); }

	private static Pref cacheExportDisplayLevel = Pref.makeIntPref("ExportDisplayLevel", tool.prefs, 0);
	/**
	 * Method to tell how to display exports.
	 * @return how to display exports.
	 * 0: full export names (the default).
	 * 1: short export names (stopping at the first nonalphabetic character).
	 * 2: exports drawn as crosses.
	 */
	public static int getExportDisplayLevel() { return cacheExportDisplayLevel.getInt(); }
	/**
	 * Method to set how to display exports.
	 * @param level how to display exports.
	 * 0: full export names (the default).
	 * 1: short export names (stopping at the first nonalphabetic character).
	 * 2: exports drawn as crosses.
	 */
	public static void setExportDisplayLevels(int level) { cacheExportDisplayLevel.setInt(level); }

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

//	private static Pref cacheAlignmentToGrid = Pref.makeDoublePref("AlignmentToGrid", tool.prefs, 1);
	/**
	 * Method to return the default alignment of objects to the grid.
	 * The default is 1, meaning that placement and movement should land on whole grid units.
	 * @return the default alignment of objects to the grid.
	 */
	public static double getAlignmentToGrid()
    {
        double[] vals = getAlignmentToGridVector();
        for (int i = 0; i < vals.length; i++)
        {
            if (vals[i] < 0) return Math.abs(vals[i]);
        }
        assert(false); // should never reach this point.
        return -1;
//        return cacheAlignmentToGrid.getDouble();
    }

//	/**
//	 * Method to set the default alignment of objects to the grid.
//	 * @param dist the default alignment of objects to the grid.
//	 */
//	public static void setAlignmentToGrid(double dist) { cacheAlignmentToGrid.setDouble(dist); }

    private static Pref cacheAlignmentToGridVector = Pref.makeStringPref("AlignmentToGridVector", tool.prefs, "(-1 0.5 0.25)");
    /**
     * Method to return the default alignment of objects to the grid.
     * The default is 1, meaning that placement and movement should land on whole grid units.
     * @return the default alignment of objects to the grid.
     */
    public static double[] getAlignmentToGridVector()
    {
        return GenMath.transformVectorIntoValues(cacheAlignmentToGridVector.getString());
    }
    /**
     * Method to set the default alignment of objects to the grid.
     * @param dist the default alignment of objects to the grid.
     */
    public static void setAlignmentToGridVector(double[] dist)
    {
        cacheAlignmentToGridVector.setString(GenMath.transformStringsIntoVector(dist[0], dist[1], dist[2]));
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

	/****************************** TEXT PREFERENCES ******************************/

	private static Pref cacheTextVisibilityNode = Pref.makeBooleanPref("TextVisibilityNode", tool.prefs, true);
	/**
	 * Method to tell whether to draw text that resides on nodes.
	 * This text includes the node name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on nodes.
	 */
	public static boolean isTextVisibilityOnNode() { return cacheTextVisibilityNode.getBoolean(); }
	/**
	 * Method to set whether to draw text that resides on nodes.
	 * This text includes the node name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on nodes.
	 */
	public static void setTextVisibilityOnNode(boolean on) { cacheTextVisibilityNode.setBoolean(on); }

	private static Pref cacheTextVisibilityArc = Pref.makeBooleanPref("TextVisibilityArc", tool.prefs, true);
	/**
	 * Method to tell whether to draw text that resides on arcs.
	 * This text includes the arc name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on arcs.
	 */
	public static boolean isTextVisibilityOnArc() { return cacheTextVisibilityArc.getBoolean(); }
	/**
	 * Method to set whether to draw text that resides on arcs.
	 * This text includes the arc name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on arcs.
	 */
	public static void setTextVisibilityOnArc(boolean on) { cacheTextVisibilityArc.setBoolean(on); }

	private static Pref cacheTextVisibilityPort = Pref.makeBooleanPref("TextVisibilityPort", tool.prefs, true);
	/**
	 * Method to tell whether to draw text that resides on ports.
	 * This text includes the port name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on ports.
	 */
	public static boolean isTextVisibilityOnPort() { return cacheTextVisibilityPort.getBoolean(); }
	/**
	 * Method to set whether to draw text that resides on ports.
	 * This text includes the port name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on ports.
	 */
	public static void setTextVisibilityOnPort(boolean on) { cacheTextVisibilityPort.setBoolean(on); }

	private static Pref cacheTextVisibilityExport = Pref.makeBooleanPref("TextVisibilityExport", tool.prefs, true);
	/**
	 * Method to tell whether to draw text that resides on exports.
	 * This text includes the export name and any parameters or attributes on it.
	 * The default is "true".
	 * @return true if the system should draw text that resides on exports.
	 */
	public static boolean isTextVisibilityOnExport() { return cacheTextVisibilityExport.getBoolean(); }
	/**
	 * Method to set whether to draw text that resides on exports.
	 * This text includes the export name and any parameters or attributes on it.
	 * @param on true if the system should draw text that resides on exports.
	 */
	public static void setTextVisibilityOnExport(boolean on) { cacheTextVisibilityExport.setBoolean(on); }

	private static Pref cacheTextVisibilityAnnotation = Pref.makeBooleanPref("TextVisibilityAnnotation", tool.prefs, true);
	/**
	 * Method to tell whether to draw text annotation text.
	 * Annotation text is not attached to any node or arc, but appears to move freely about the cell.
	 * In implementation, they are displayable Variables on Generic:invisible-pin nodes.
	 * The default is "true".
	 * @return true if the system should draw annotation text.
	 */
	public static boolean isTextVisibilityOnAnnotation() { return cacheTextVisibilityAnnotation.getBoolean(); }
	/**
	 * Method to set whether to draw annotation text.
	 * Annotation text is not attached to any node or arc, but appears to move freely about the cell.
	 * In implementation, they are displayable Variables on Generic:invisible-pin nodes.
	 * @param on true if the system should draw annotation text.
	 */
	public static void setTextVisibilityOnAnnotation(boolean on) { cacheTextVisibilityAnnotation.setBoolean(on); }

	private static Pref cacheTextVisibilityInstance = Pref.makeBooleanPref("TextVisibilityInstance", tool.prefs, true);
	/**
	 * Method to tell whether to draw the name of on cell instances.
	 * The default is "true".
	 * @return true if the system should draw the name of on cell instances.
	 */
	public static boolean isTextVisibilityOnInstance() { return cacheTextVisibilityInstance.getBoolean(); }
	/**
	 * Method to set whether to draw the name of on cell instances.
	 * @param on true if the system should draw the name of on cell instances.
	 */
	public static void setTextVisibilityOnInstance(boolean on) { cacheTextVisibilityInstance.setBoolean(on); }

	private static Pref cacheTextVisibilityCell = Pref.makeBooleanPref("TextVisibilityCell", tool.prefs, true);
	/**
	 * Method to tell whether to draw text that resides on the cell.
	 * This includes the current cell's parameters or attributes (for example, spice templates).
	 * The default is "true".
	 * @return true if the system should draw text that resides on the cell.
	 */
	public static boolean isTextVisibilityOnCell() { return cacheTextVisibilityCell.getBoolean(); }
	/**
	 * Method to set whether to draw text that resides on the cell.
	 * This includes the current cell's parameters or attributes (for example, spice templates).
	 * @param on true if the system should draw text that resides on the cell.
	 */
	public static void setTextVisibilityOnCell(boolean on) { cacheTextVisibilityCell.setBoolean(on); }

	private static Pref cacheSmartVerticalPlacementExport = Pref.makeIntPref("SmartVerticalPlacementExport", tool.prefs, 0);
	/**
	 * Method to tell what type of "smart" vertical text placement should be done for Exports.
	 * The values can be 0: no smart placement; 1: place text "inside"; 2: place text "outside".
	 * The default is 0.
	 * @return the type of "smart" vertical text placement to be done for Exports.
	 */
	public static int getSmartVerticalPlacementExport() { return cacheSmartVerticalPlacementExport.getInt(); }
	/**
	 * Method to set the type of "smart" vertical text placement to be done for Exports.
	 * The values can be 0: no smart placement; 1: place text "inside"; 2: place text "outside".
	 * @param s the type of "smart" vertical text placement to be done for Exports.
	 */
	public static void setSmartVerticalPlacementExport(int s) { cacheSmartVerticalPlacementExport.setInt(s); }

	private static Pref cacheSmartHorizontalPlacementExport = Pref.makeIntPref("SmartHorizontalPlacementExport", tool.prefs, 0);
	/**
	 * Method to tell what type of "smart" horizontal text placement should be done for Exports.
	 * The values can be 0: no smart placement; 1: place text "inside"; 2: place text "outside".
	 * The default is 0.
	 * @return the type of "smart" horizontal text placement to be done for Exports.
	 */
	public static int getSmartHorizontalPlacementExport() { return cacheSmartHorizontalPlacementExport.getInt(); }
	/**
	 * Method to set the type of "smart" horizontal text placement to be done for Exports.
	 * The values can be 0: no smart placement; 1: place text "inside"; 2: place text "outside".
	 * @param s the type of "smart" horizontal text placement to be done for Exports.
	 */
	public static void setSmartHorizontalPlacementExport(int s) { cacheSmartHorizontalPlacementExport.setInt(s); }

	private static Pref cacheSmartVerticalPlacementArc = Pref.makeIntPref("SmartVerticalPlacementArc", tool.prefs, 0);
	/**
	 * Method to tell what type of "smart" text placement should be done for vertical Arcs.
	 * The values can be 0: place text inside; 1: place text to left; 2: place text to right.
	 * The default is 0.
	 * @return the type of "smart" text placement to be done for vertical Arcs.
	 */
	public static int getSmartVerticalPlacementArc() { return cacheSmartVerticalPlacementArc.getInt(); }
	/**
	 * Method to set the type of "smart" text placement to be done for vertical Arcs.
	 * The values can be 0: place text inside; 1: place text to left; 2: place text to right.
	 * @param s the type of "smart" text placement to be done for vertical Arcs.
	 */
	public static void setSmartVerticalPlacementArc(int s) { cacheSmartVerticalPlacementArc.setInt(s); }

	private static Pref cacheSmartHorizontalPlacementArc = Pref.makeIntPref("SmartHorizontalPlacementArc", tool.prefs, 0);
	/**
	 * Method to tell what type of "smart" text placement should be done for horizontal Arcs.
	 * The values can be 0: place text inside; 1: place text above; 2: place text below.
	 * The default is 0.
	 * @return the type of "smart" text placement to be done for horizontal Arcs.
	 */
	public static int getSmartHorizontalPlacementArc() { return cacheSmartHorizontalPlacementArc.getInt(); }
	/**
	 * Method to set the type of "smart" text placement to be done for horizontal Arcs.
	 * The values can be 0: place text inside; 1: place text above; 2: place text below.
	 * @param s the type of "smart" text placement to be done for horizontal Arcs.
	 */
	public static void setSmartHorizontalPlacementArc(int s) { cacheSmartHorizontalPlacementArc.setInt(s); }

	private static Pref cacheDefaultFont = Pref.makeStringPref("DefaultFont", tool.prefs, "SansSerif");
	/**
	 * Method to get the default font to use on the display.
	 * The default is "SansSerif".
	 * @return the default font to use on the display.
	 */
	public static String getDefaultFont() { return cacheDefaultFont.getString(); }
	/**
	 * Method to set the default font to use on the display.
	 * @param f the default font to use on the display.
	 */
	public static void setDefaultFont(String f) { cacheDefaultFont.setString(f); }

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

	private static Pref cacheGlobalTextScale = Pref.makeDoublePref("TextGlobalScale", tool.prefs, 1);
	/**
	 * Method to tell the global text scale factor.
	 * This factor enlarges or reduces all displayed text.
	 * The default is "1".
	 * @return the global text scale factor.
	 */
	public static double getGlobalTextScale() { return cacheGlobalTextScale.getDouble(); }
	/**
	 * Method to set the global text scale factor.
	 * This factor enlarges or reduces all displayed text.
	 * @param s the global text scale.
	 */
	public static void setGlobalTextScale(double s) { cacheGlobalTextScale.setDouble(s); }

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

	/****************************** COLOR PREFERENCES ******************************/

	private static Pref cacheColorBackground = Pref.makeIntPref("ColorBackground", tool.prefs, Color.LIGHT_GRAY.getRGB());
	/**
	 * Method to get the color of the background on the display.
	 * The default is "light gray".
	 * @return the color of the background on the display.
	 */
	public static int getColorBackground() { return cacheColorBackground.getInt(); }
	/**
	 * Method to set the color of the background on the display.
	 * @param c the color of the background on the display.
	 */
	public static void setColorBackground(int c)
	{
		cacheColorBackground.setInt(c);
//        Color color = new Color(c);

//		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
//		{
//			WindowFrame wf = it.next();
//			if (wf.getContent() instanceof EditWindow)
//			{
//				EditWindow wnd = (EditWindow)wf.getContent();
//				PixelDrawing offscreen = wnd.getOffscreen();
//				offscreen.setBackgroundColor(color);
//			}
//		}

        // 3D case. Uses observer/observable pattern so doesn't make sense to call every single 3D ViewWindow
        // and update
        try
        {
            Class j3DUtilsClass = Resources.get3DClass("utils.J3DUtils");
            Method setMethod = j3DUtilsClass.getDeclaredMethod("setBackgroundColor", new Class[] {Object.class});
            setMethod.invoke(j3DUtilsClass, new Object[]{null});
        } catch (Exception e) {
            System.out.println("Cannot call 3D plugin method setBackgroundColor: " + e.getMessage());
//            e.printStackTrace();
        }
	}

	private static Pref cacheColorGrid = Pref.makeIntPref("ColorGrid", tool.prefs, Color.BLACK.getRGB());
	/**
	 * Method to get the color of the grid on the display.
	 * The default is "black".
	 * @return the color of the grid on the display.
	 */
	public static int getColorGrid() { return cacheColorGrid.getInt(); }
	/**
	 * Method to set the color of the grid on the display.
	 * @param c the color of the grid on the display.
	 */
	public static void setColorGrid(int c) { cacheColorGrid.setInt(c); }

	private static Pref cacheColorHighlight = Pref.makeIntPref("ColorHighlight", tool.prefs, Color.WHITE.getRGB());
	/**
	 * Method to get the color of the highlight on the display.
	 * The default is "white".
	 * @return the color of the highlight on the display.
	 */
	public static int getColorHighlight() { return cacheColorHighlight.getInt(); }
	/**
	 * Method to set the color of the highlight on the display.
	 * @param c the color of the highlight on the display.
	 */
	public static void setColorHighlight(int c) { cacheColorHighlight.setInt(c); }

    private static Pref cacheColorMouseOverHighlight = Pref.makeIntPref("ColorMouseOverHighlight", tool.prefs, (new Color(51,255,255)).getRGB());
    /**
     * Method to get the color of the highlight on the display.
     * The default is "white".
     * @return the color of the highlight on the display.
     */
    public static int getColorMouseOverHighlight() { return cacheColorMouseOverHighlight.getInt(); }
    /**
     * Method to set the color of the highlight on the display.
     * @param c the color of the highlight on the display.
     */
    public static void setColorMouseOverHighlight(int c) { cacheColorMouseOverHighlight.setInt(c); }

	private static Pref cacheColorPortHighlight = Pref.makeIntPref("ColorPortHighlight", tool.prefs, Color.YELLOW.getRGB());
	/**
	 * Method to get the color of the port highlight on the display.
	 * The default is "yellow".
	 * @return the color of the port highlight on the display.
	 */
	public static int getColorPortHighlight() { return cacheColorPortHighlight.getInt(); }
	/**
	 * Method to set the color of the port highlight on the display.
	 * @param c the color of the port highlight on the display.
	 */
	public static void setColorPortHighlight(int c) { cacheColorPortHighlight.setInt(c); }

	private static Pref cacheColorText = Pref.makeIntPref("ColorText", tool.prefs, Color.BLACK.getRGB());
	/**
	 * Method to get the color of the text on the display.
	 * The default is "black".
	 * @return the color of the text on the display.
	 */
	public static int getColorText() { return cacheColorText.getInt(); }
	/**
	 * Method to set the color of the text on the display.
	 * @param c the color of the text on the display.
	 */
	public static void setColorText(int c) { cacheColorText.setInt(c); }

	private static Pref cacheColorInstanceOutline = Pref.makeIntPref("ColorInstanceOutline", tool.prefs, Color.BLACK.getRGB());
	/**
	 * Method to get the color of the instance outlines on the display.
	 * The default is "black".
	 * @return the color of the instance outlines on the display.
	 */
	public static int getColorInstanceOutline() { return cacheColorInstanceOutline.getInt(); }
	/**
	 * Method to set the color of the instance outlines on the display.
	 * @param c the color of the instance outlines on the display.
	 */
	public static void setColorInstanceOutline(int c) { cacheColorInstanceOutline.setInt(c); }

	private static Pref cacheColorWaveformBackground = Pref.makeIntPref("ColorWaveformBackground", tool.prefs, Color.BLACK.getRGB());
	/**
	 * Method to get the color of the waveform window background.
	 * The default is "black".
	 * @return the color of the waveform window background.
	 */
	public static int getColorWaveformBackground() { return cacheColorWaveformBackground.getInt(); }
	/**
	 * Method to set the color of the waveform window background.
	 * @param c the color of the waveform window background.
	 */
	public static void setColorWaveformBackground(int c) { cacheColorWaveformBackground.setInt(c); }

	private static Pref cacheColorWaveformForeground = Pref.makeIntPref("ColorWaveformForeground", tool.prefs, Color.WHITE.getRGB());
	/**
	 * Method to get the color of the waveform window foreground.
	 * This includes lines and text.
	 * The default is "white".
	 * @return the color of the traces in waveform windows.
	 */
	public static int getColorWaveformForeground() { return cacheColorWaveformForeground.getInt(); }
	/**
	 * Method to set the color of the waveform window foreground.
	 * This includes lines and text.
	 * @param c the color of the waveform window foreground.
	 */
	public static void setColorWaveformForeground(int c) { cacheColorWaveformForeground.setInt(c); }

	private static Pref cacheColorWaveformStimuli = Pref.makeIntPref("ColorWaveformStimuli", tool.prefs, Color.RED.getRGB());
	/**
	 * Method to get the color of the traces in waveform windows.
	 * Applies only when not a "multistate" display, which uses many colors.
	 * The default is "red".
	 * @return the color of the traces in waveform windows.
	 */
	public static int getColorWaveformStimuli() { return cacheColorWaveformStimuli.getInt(); }
	/**
	 * Method to set the color of the traces in waveform windows.
	 * Applies only when not a "multistate" display, which uses many colors.
	 * @param c the color of the traces in waveform windows.
	 */
	public static void setColorWaveformStimuli(int c) { cacheColorWaveformStimuli.setInt(c); }

	private static Pref cacheColorWaveformStrengthOff = Pref.makeIntPref("ColorWaveformStrengthOff", tool.prefs, Color.BLUE.getRGB());
	/**
	 * Method to get the color of waveform window traces that have "off" strength.
	 * The default is "blue".
	 * @return the color of waveform window traces that have "off" strength.
	 */
	public static int getColorWaveformStrengthOff() { return cacheColorWaveformStrengthOff.getInt(); }
	/**
	 * Method to set the color of waveform window traces that have "off" strength.
	 * @param c the color of waveform window traces that have "off" strength.
	 */
	public static void setColorWaveformStrengthOff(int c) { cacheColorWaveformStrengthOff.setInt(c); }

	private static Pref cacheColorWaveformStrengthNode = Pref.makeIntPref("ColorWaveformStrengthNode", tool.prefs, Color.GREEN.getRGB());
	/**
	 * Method to get the color of waveform window traces that have "node" strength.
	 * The default is "green".
	 * @return the color of waveform window traces that have "node" strength.
	 */
	public static int getColorWaveformStrengthNode() { return cacheColorWaveformStrengthNode.getInt(); }
	/**
	 * Method to set the color of waveform window traces that have "node" strength.
	 * @param c the color of waveform window traces that have "node" strength.
	 */
	public static void setColorWaveformStrengthNode(int c) { cacheColorWaveformStrengthNode.setInt(c); }

	private static Pref cacheColorWaveformStrengthGate = Pref.makeIntPref("ColorWaveformStrengthGate", tool.prefs, Color.MAGENTA.getRGB());
	/**
	 * Method to get the color of waveform window traces that have "gate" strength.
	 * The default is "magenta".
	 * @return the color of waveform window traces that have "gate" strength.
	 */
	public static int getColorWaveformStrengthGate() { return cacheColorWaveformStrengthGate.getInt(); }
	/**
	 * Method to set the color of waveform window traces that have "gate" strength.
	 * @param c the color of waveform window traces that have "gate" strength.
	 */
	public static void setColorWaveformStrengthGate(int c) { cacheColorWaveformStrengthGate.setInt(c); }

	private static Pref cacheColorWaveformStrengthPower = Pref.makeIntPref("ColorWaveformStrengthPower", tool.prefs, Color.LIGHT_GRAY.getRGB());
	/**
	 * Method to get the color of waveform window traces that have "power" strength.
	 * The default is "light gray".
	 * @return the color of waveform window traces that have "power" strength.
	 */
	public static int getColorWaveformStrengthPower() { return cacheColorWaveformStrengthPower.getInt(); }
	/**
	 * Method to set the color of waveform window traces that have "power" strength.
	 * @param c the color of waveform window traces that have "power" strength.
	 */
	public static void setColorWaveformStrengthPower(int c) { cacheColorWaveformStrengthPower.setInt(c); }

	private static Pref cacheColorWaveformCrossProbeLow = Pref.makeIntPref("ColorWaveformCrossProbeLow", tool.prefs, Color.BLUE.getRGB());
	/**
	 * Method to get the color of cross-probe traces from the waveform window that are "low".
	 * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
	 * The default is "blue".
	 * @return the color of cross-probe traces from the waveform window that are "low".
	 */
	public static int getColorWaveformCrossProbeLow() { return cacheColorWaveformCrossProbeLow.getInt(); }
	/**
	 * Method to set the color of cross-probe traces from the waveform window that are "low".
	 * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
	 * @param c the color of cross-probe traces from the waveform window that are "low".
	 */
	public static void setColorWaveformCrossProbeLow(int c) { cacheColorWaveformCrossProbeLow.setInt(c); }

	private static Pref cacheColorWaveformCrossProbeHigh = Pref.makeIntPref("ColorWaveformCrossProbeHigh", tool.prefs, Color.GREEN.getRGB());
	/**
	 * Method to get the color of cross-probe traces from the waveform window that are "high".
	 * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
	 * The default is "green".
	 * @return the color of cross-probe traces from the waveform window that are "high".
	 */
	public static int getColorWaveformCrossProbeHigh() { return cacheColorWaveformCrossProbeHigh.getInt(); }
	/**
	 * Method to set the color of cross-probe traces from the waveform window that are "high".
	 * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
	 * @param c the color of cross-probe traces from the waveform window that are "high".
	 */
	public static void setColorWaveformCrossProbeHigh(int c) { cacheColorWaveformCrossProbeHigh.setInt(c); }

	private static Pref cacheColorWaveformCrossProbeX = Pref.makeIntPref("ColorWaveformCrossProbeX", tool.prefs, Color.BLACK.getRGB());
	/**
	 * Method to get the color of cross-probe traces from the waveform window that are "undefined".
	 * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
	 * The default is "black".
	 * @return the color of cross-probe traces from the waveform window that are "undefined".
	 */
	public static int getColorWaveformCrossProbeX() { return cacheColorWaveformCrossProbeX.getInt(); }
	/**
	 * Method to set the color of cross-probe traces from the waveform window that are "undefined".
	 * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
	 * @param c the color of cross-probe traces from the waveform window that are "undefined".
	 */
	public static void setColorWaveformCrossProbeX(int c) { cacheColorWaveformCrossProbeX.setInt(c); }

	private static Pref cacheColorWaveformCrossProbeZ = Pref.makeIntPref("ColorWaveformCrossProbeZ", tool.prefs, Color.LIGHT_GRAY.getRGB());
	/**
	 * Method to get the color of cross-probe traces from the waveform window that are "floating".
	 * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
	 * The default is "light gray".
	 * @return the color of cross-probe traces from the waveform window that are "floating".
	 */
	public static int getColorWaveformCrossProbeZ() { return cacheColorWaveformCrossProbeZ.getInt(); }
	/**
	 * Method to set the color of cross-probe traces from the waveform window that are "floating".
	 * These are lines drawn on the schematic or layout to correspond with the value in the waveform window.
	 * @param c the color of cross-probe traces from the waveform window that are "floating".
	 */
	public static void setColorWaveformCrossProbeZ(int c) { cacheColorWaveformCrossProbeZ.setInt(c); }

	/****************************** UNITS PREFERENCES ******************************/

	private static Pref cacheDistanceUnits = Pref.makeIntPref("DistanceUnits", tool.prefs, TextUtils.UnitScale.NANO.getIndex());
	/**
	 * Method to get current distance units.
	 * The default is "nanometers".
	 * @return the current distance units.
	 */
	public static TextUtils.UnitScale getDistanceUnits()
	{
		return TextUtils.UnitScale.findFromIndex(cacheDistanceUnits.getInt());
	}
	/**
	 * Method to set the current distance units.
	 * @param us the current distance units.
	 */
	public static void setDistanceUnits(TextUtils.UnitScale us)
	{
		cacheDistanceUnits.setInt(us.getIndex());
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

	private static Pref cacheTimeUnits = Pref.makeIntPref("TimeUnits", tool.prefs, TextUtils.UnitScale.NONE.getIndex());
	/**
	 * Method to get current time units.
	 * The default is "seconds".
	 * @return the current time) units.
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

	/****************************** MISCELLANEOUS PREFERENCES ******************************/

    private static Pref cacheDefaultTechnology = Pref.makeStringSetting("DefaultTechnology", tool.prefs, tool,
            tool.getProjectSettings(), null,
        "Technology tab", "Default Technology for editing", "mocmos");
	/**
	 * Method to get default technique in Tech Palette.
	 * The default is "mocmos".
	 * @return the default technology to use in Tech Palette
	 */
	public static String getDefaultTechnology() { return cacheDefaultTechnology.getString(); }
	/**
	 * Method to set default technique in Tech Palette.
	 * @param t the default technology to use in Tech Palette.
	 */
	public static void setDefaultTechnology(String t) { cacheDefaultTechnology.setString(t); }

    private static Pref cacheSchematicTechnology = Pref.makeStringSetting("SchematicTechnology", tool.prefs, tool,
            tool.getProjectSettings(), null,
        "Technology tab", "Schematics use scale values from this technology", "mocmos");
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
        String t = cacheSchematicTechnology.getString();
		Technology tech = Technology.findTechnology(t);
        if (tech == null) return MoCMOS.tech;
        return tech;
    }
	/**
	 * Method to set the layout Technology to use when schematics are found.
	 * This is important in Spice deck generation (for example) because the Spice primitives may
	 * say "2x3" on them, but a real technology (such as "mocmos") must be found to convert these pure
	 * numbers to real spacings for the deck.
	 * @param t the Technology to use when schematics are found.
	 */
	public static void setSchematicTechnology(Technology t)
	{
        if (t == null) return;
        cacheSchematicTechnology.setString(t.getTechName());
    }

//    public static final String INITIALWORKINGDIRSETTING_BASEDONOS = "Based on OS";
//    public static final String INITIALWORKINGDIRSETTING_USECURRENTDIR = "Use current directory";
//    public static final String INITIALWORKINGDIRSETTING_USELASTDIR = "Use last used directory";
//    private static final String [] initialWorkingDirectorySettingChoices = {INITIALWORKINGDIRSETTING_BASEDONOS, INITIALWORKINGDIRSETTING_USECURRENTDIR, INITIALWORKINGDIRSETTING_USELASTDIR};
//    private static Pref cacheInitialWorkingDirectorySetting = Pref.makeStringPref("InitialWorkingDirectorySetting", tool.prefs, initialWorkingDirectorySettingChoices[0]);
//
//    /**
//     * Method to get the way Electric chooses the initial working directory
//     * @return a string describing the way Electric chooses the initial working directory
//     */
//    public static String getInitialWorkingDirectorySetting() { return cacheInitialWorkingDirectorySetting.getString(); }
//
//    /**
//     * Method to set the way Electric chooses the initial working directory
//     * @param setting one of the String settings from getInitialWorkingDirectorySettings
//     */
//    public static void setInitialWorkingDirectorySetting(String setting) {
//        for (int i=0; i<initialWorkingDirectorySettingChoices.length; i++) {
//            if ((initialWorkingDirectorySettingChoices[i]).equals(setting)) {
//                cacheInitialWorkingDirectorySetting.setString(setting);
//            }
//        }
//    }
//
//    /**
//     * Get the choices for the way Electric chooses the initial working directory
//     * @return an iterator over a list of strings that can be used with setIntialWorkingDirectorySetting()
//     */
//    public static Iterator<String> getInitialWorkingDirectorySettings() {
//        ArrayList<String> list = new ArrayList<String>();
//        for (int i=0; i<initialWorkingDirectorySettingChoices.length; i++)
//            list.add(initialWorkingDirectorySettingChoices[i]);
//        return list.iterator();
//    }

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

	private static Pref cachePlayClickSoundsWhenCreatingArcs = Pref.makeBooleanPref("PlayClickSoundsWhenCreatingArcs", tool.prefs, true);
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

	private static Pref cacheIncludeDateAndVersionInOutput = Pref.makeBooleanSetting("IncludeDateAndVersionInOutput", tool.prefs, tool,
            tool.getProjectSettings(), null,
		"Netlists tab", "Include date and version in output", true);
	/**
	 * Method to tell whether to include the date and Electric version in output files.
	 * The default is "true".
	 * @return true if the system should include the date and Electric version in output files.
	 */
	public static boolean isIncludeDateAndVersionInOutput() { return cacheIncludeDateAndVersionInOutput.getBoolean(); }
	/**
	 * Method to set whether to include the date and Electric version in output files.
	 * @param on true if the system should include the date and Electric version in output files.
	 */
	public static void setIncludeDateAndVersionInOutput(boolean on) { cacheIncludeDateAndVersionInOutput.setBoolean(on); }

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

	private static Pref cachePatternedScaleLimit = Pref.makeDoublePref("PatternedScaleLimit", tool.prefs, 0.1);
	/**
	 * Method to tell the scale of EditWindow when use patterned drawing.
	 * Smaller scales use solid drawing.
	 * The default is 0.1, meaning that 10 lamdas per pixel.
	 * @return the scale of EditWindow when use patterned drawing.
	 */
	public static double getPatternedScaleLimit() { return cachePatternedScaleLimit.getDouble(); }
	/**
	 * Method to set the scale of EditWindow when use patterned drawing.
	 * Smaller scales use solid drawing.
     * @param l the scale of EditWindow when use patterned drawing.
	 */
	public static void setPatternedScaleLimit(double l) { cachePatternedScaleLimit.setDouble(l); }

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

	private static Pref cacheErrorLimit = Pref.makeIntPref("ErrorLimit", tool.prefs, 0);
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

    private static Pref cacheMaxUndoHistory = Pref.makeIntPref("MaxUndoHistory", tool.prefs, 40);
    /**
     * Method to get the maximum number of undos retained in memory
     */
    public static int getMaxUndoHistory() { return cacheMaxUndoHistory.getInt(); }
    /**
     * Method to set the maximum number of undos retained in memory
     */
    public static void setMaxUndoHistory(int n) { cacheMaxUndoHistory.setInt(n); }

	private static Pref cacheMemorySize = Pref.makeIntPref("MemorySize", tool.prefs, 1000);
	/**
	 * Method to tell the maximum memory to use for Electric, in megatybes.
	 * The default is 1000 (1 gigabyte).
	 * @return the maximum memory to use for Electric (in megabytes).
	 */
	public static int getMemorySize() { return cacheMemorySize.getInt(); }
	/**
	 * Method to set the maximum memory to use for Electric.
	 * @param limit maximum memory to use for Electric (in megabytes).
	 */
	public static void setMemorySize(int limit) { cacheMemorySize.setInt(limit); }

	private static Pref cachePermSize = Pref.makeIntPref("PermSize", tool.prefs, 0);
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

	private static Pref cacheUseClientServer = Pref.makeBooleanPref("UseClientServer", tool.prefs, false);
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

	private static Pref cacheSnapshotLogging = Pref.makeBooleanPref("SnapshotLogging", tool.prefs, false);
	/**
	 * Method to tell whether to perform snapshot logging in a temporary file.
	 * The default is "false".
	 * @return true to perform snapshot logging in a temporary file
	 */
	public static boolean isSbapshotLogging() { return cacheSnapshotLogging.getBoolean(); }
	/**
	 * Method to set whether to perform snapshot logging in a temporary file
	 * @param on true to perform snapshot logging iu a temporary file
	 */
	public static void setSnapshotLogging(boolean on) { cacheSnapshotLogging.setBoolean(on); }

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

	private static Pref cachePlaceCellCenter = Pref.makeBooleanPref("PlaceCellCenter", tool.prefs, true);
	/**
	 * Method to tell whether to place a Cell-Center primitive in every newly created Cell.
	 * The default is "true".
	 * @return true if the system should place a Cell-Center primitive in every newly created Cell.
	 */
	public static boolean isPlaceCellCenter() { return cachePlaceCellCenter.getBoolean(); }
	/**
	 * Method to set whether to place a Cell-Center primitive in every newly created Cell.
	 * @param on true if the system should place a Cell-Center primitive in every newly created Cell.
	 */
	public static void setPlaceCellCenter(boolean on) { cachePlaceCellCenter.setBoolean(on); }

	private static Pref cacheReconstructArcsToDeletedCells = Pref.makeBooleanPref("ReconstructArcsToDeletedCells", tool.prefs, true);
	/**
	 * Method to tell whether to reconstruct arcs to deleted cell instances.
	 * When true, deleting a cell instance will leave the connecting arcs in place (now terminated with pins).
	 * The default is "true".
	 * @return true if the system should reconstruct arcs to deleted cell instances.
	 */
	public static boolean isReconstructArcsToDeletedCells() { return cacheReconstructArcsToDeletedCells.getBoolean(); }
	/**
	 * Method to set whether to reconstruct arcs to deleted cell instances.
	 * When true, deleting a cell instance will leave the connecting arcs in place (now terminated with pins).
	 * @param on true if the system should reconstruct arcs to deleted cell instances.
	 */
	public static void setReconstructArcsToDeletedCells(boolean on) { cacheReconstructArcsToDeletedCells.setBoolean(on); }

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

	private static Pref cacheDisallowModificationLockedPrims = Pref.makeBooleanPref("DisallowModificationLockedPrims", tool.prefs, false);
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

	private static Pref cacheDisallowModificationComplexNodes = Pref.makeBooleanPref("DisallowModificationComplexNodes", tool.prefs, false);
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
}
