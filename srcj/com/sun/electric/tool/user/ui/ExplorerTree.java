/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExplorerTree.java
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
package com.sun.electric.tool.user.ui;

import com.sun.electric.Main;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.simulation.Simulation;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.ChangeCellGroup;
import com.sun.electric.tool.user.dialogs.NewCell;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.menus.CellMenu;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Class to display a cell explorer tree-view of the database.
 */
public class ExplorerTree extends JTree implements DragGestureListener, DragSourceListener
{
	private TreeHandler handler = null;
	private DefaultMutableTreeNode rootNode;
	private DefaultTreeModel treeModel;

	private static final int SHOWALPHABETICALLY = 1;
	private static final int SHOWBYCELLGROUP    = 2;
	private static final int SHOWBYHIERARCHY    = 3;
	private static int howToShow = SHOWBYCELLGROUP;

	private static ImageIcon iconLibrary = null;
	private static ImageIcon iconGroup = null;
	private static ImageIcon iconJobs = null;
	private static ImageIcon iconLibraries = null;
	private static ImageIcon iconErrors = null;
	private static ImageIcon iconErrorMsg = null;
    private static ImageIcon iconWarnMsg = null;
	private static ImageIcon iconSignals = null;
	private static ImageIcon iconSweeps = null;
	private static ImageIcon iconViewIcon = null;
	private static ImageIcon iconViewOldIcon = null;
	private static ImageIcon iconViewLayout = null;
	private static ImageIcon iconViewOldLayout = null;
	private static ImageIcon iconViewMultiPageSchematics = null;
	private static ImageIcon iconViewSchematics = null;
	private static ImageIcon iconViewOldSchematics = null;
	private static ImageIcon iconViewMisc = null;
	private static ImageIcon iconViewOldMisc = null;
	private static ImageIcon iconViewText = null;
	private static ImageIcon iconViewOldText = null;

	private static class CellAndCount
	{
		private Cell cell;
		private int count;
		public CellAndCount(Cell cell, int count) { this.cell = cell;   this.count = count; }
		public Cell getCell() { return cell; }
		public int getCount() { return count; }
	}

	private static class MultiPageCell
	{
		private Cell cell;
		private int pageNo;
	}

	/**
	 * Method to create a new ExplorerTree.
	 * @param treeModel the tree to display.
	 * @return the newly created ExplorerTree.
	 */
	public static ExplorerTree CreateExplorerTree(DefaultMutableTreeNode rootNode, DefaultTreeModel treeModel)
	{
		ExplorerTree tree = new ExplorerTree(rootNode, treeModel);
		tree.handler = new TreeHandler(tree);
		tree.addMouseListener(tree.handler);
		return tree;
	}

	private ExplorerTree(DefaultMutableTreeNode rootNode, DefaultTreeModel treeModel)
	{
		super(treeModel);
		this.rootNode = rootNode;
		this.treeModel = treeModel;

		initDND();

		// single selection as default
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// do not show top-level
		setRootVisible(false);
		setShowsRootHandles(true);
		setToggleClickCount(3);

		// enable tool tips - we'll use these to display useful info
		ToolTipManager.sharedInstance().registerComponent(this);

		// register our own extended renderer for custom icons and tooltips
		setCellRenderer(new MyRenderer());
	}

	/**
	 * Method to return the currently selected object in the explorer tree.
	 * @return the currently selected object in the explorer tree.
	 */
	public Object getCurrentlySelectedObject() { return handler.currentSelectedObject; }

	/**
	 * A static object is used so that its open/closed tree state can be maintained.
	 */
	private static String libraryNode = "LIBRARIES";

	public static DefaultMutableTreeNode makeLibraryTree()
	{
		DefaultMutableTreeNode libraryExplorerTree = new DefaultMutableTreeNode(libraryNode);

		// reconstruct the tree
		switch (howToShow)
		{
			case SHOWALPHABETICALLY:
				rebuildExplorerTreeByName(libraryExplorerTree);
				break;
			case SHOWBYCELLGROUP:
				rebuildExplorerTreeByGroups(libraryExplorerTree);
				break;
			case SHOWBYHIERARCHY:
				rebuildExplorerTreeByHierarchy(libraryExplorerTree);
				break;
		}
		return libraryExplorerTree;
	}

	private static synchronized void rebuildExplorerTreeByName(DefaultMutableTreeNode libraryExplorerTree)
	{
		/*for(Library lib: Library.getVisibleLibraries())*/
		for(Iterator it = Library.getVisibleLibraries().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			for(Iterator eit = lib.getCells(); eit.hasNext(); )
			{
				Cell cell = (Cell)eit.next();
				DefaultMutableTreeNode cellTree = new DefaultMutableTreeNode(cell);
				libTree.add(cellTree);
			}
			libraryExplorerTree.add(libTree);
		}
	}

	private static synchronized void rebuildExplorerTreeByHierarchy(DefaultMutableTreeNode libraryExplorerTree)
	{
		/*for(Library lib: Library.getVisibleLibraries())*/
		for(Iterator it = Library.getVisibleLibraries().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			for(Iterator eit = lib.getCells(); eit.hasNext(); )
			{
				Cell cell = (Cell)eit.next();

				// ignore icons and text views
				if (cell.isIcon()) continue;
				if (cell.getView().isTextView()) continue;

		        HashSet addedCells = new HashSet();
				for(Iterator vIt = cell.getVersions(); vIt.hasNext(); )
				{
					Cell cellVersion = (Cell)vIt.next();
					Iterator insts = cellVersion.getInstancesOf();
					if (insts.hasNext()) continue;

					// no children: add this as root node
                    if (addedCells.contains(cellVersion)) continue;          // prevent duplicate entries
					DefaultMutableTreeNode cellTree = new DefaultMutableTreeNode(cellVersion);
					libTree.add(cellTree);
                    addedCells.add(cellVersion);
					createHierarchicalExplorerTree(cellVersion, cellTree);
				}
			}
			libraryExplorerTree.add(libTree);
		}
	}

	/**
	 * Method to build a hierarchical explorer structure.
	 */
	private static void createHierarchicalExplorerTree(Cell cell, DefaultMutableTreeNode cellTree)
	{
		// see what is inside
		HashMap cellCount = new HashMap();
		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (!(ni.getProto() instanceof Cell)) continue;
			Cell subCell = (Cell)ni.getProto();
			if (subCell.isIcon())
			{
				if (ni.isIconOfParent()) continue;
				subCell = subCell.contentsView();
				if (subCell == null) continue;
			}
			DBMath.MutableInteger mi = (DBMath.MutableInteger)cellCount.get(subCell);
			if (mi == null)
			{
				mi = new DBMath.MutableInteger(0);
				cellCount.put(subCell, mi);
			}
			mi.setValue(mi.intValue()+1);
		}

		// show what is there
		for(Iterator it = cellCount.keySet().iterator(); it.hasNext(); )
		{
			Cell subCell = (Cell)it.next();
			DBMath.MutableInteger mi = (DBMath.MutableInteger)cellCount.get(subCell);
			if (mi == null) continue;

			CellAndCount cc = new CellAndCount(subCell, mi.intValue());
			DefaultMutableTreeNode subCellTree = new DefaultMutableTreeNode(cc);
			cellTree.add(subCellTree);
			createHierarchicalExplorerTree(subCell, subCellTree);
		}
	}

	private static synchronized void rebuildExplorerTreeByGroups(DefaultMutableTreeNode libraryExplorerTree)
	{
		HashSet cellsSeen = new HashSet();
		/*for(Library lib: Library.getVisibleLibraries())*/
		for(Iterator it = Library.getVisibleLibraries().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			for(Iterator eit = lib.getCells(); eit.hasNext(); )
			{
				Cell cell = (Cell)eit.next();
				cellsSeen.remove(cell);
			}
			for(Iterator eit = lib.getCells(); eit.hasNext(); )
			{
				Cell cell = (Cell)eit.next();
				if (cell.getNewestVersion() != cell) continue;
				Cell.CellGroup group = cell.getCellGroup();
				int numNewCells = 0;
				for(Iterator gIt = group.getCells(); gIt.hasNext(); )
				{
					Cell cellInGroup = (Cell)gIt.next();
					if (cellInGroup.getNewestVersion() == cellInGroup) numNewCells++;
				}
				if (numNewCells == 1)
				{
					addCellAndAllVersions(cell, libTree);
					continue;
				}

				List cellsInGroup = group.getCellsSortedByView();
				DefaultMutableTreeNode groupTree = null;
				for(Iterator gIt = cellsInGroup.iterator(); gIt.hasNext(); )
				{
					Cell cellInGroup = (Cell)gIt.next();
                    if ((cellInGroup.getNumVersions() > 1) && (cellInGroup.getNewestVersion() != cellInGroup)) continue;
					if (cellsSeen.contains(cellInGroup)) continue;
					if (groupTree == null)
					{
						groupTree = new DefaultMutableTreeNode(group);
					}
					cellsSeen.add(cellInGroup);
					addCellAndAllVersions(cellInGroup, groupTree);
				}
				if (groupTree != null)
					libTree.add(groupTree);
			}
			libraryExplorerTree.add(libTree);
		}
	}

	private static void addCellAndAllVersions(Cell cell, DefaultMutableTreeNode libTree)
	{
		DefaultMutableTreeNode cellTree = new DefaultMutableTreeNode(cell);
		libTree.add(cellTree);
		if (cell.isMultiPage())
		{
			int pageCount = cell.getNumMultiPages();
			for(int i=0; i<pageCount; i++)
			{
				MultiPageCell mpc = new MultiPageCell();
				mpc.cell = cell;
				mpc.pageNo = i;
				DefaultMutableTreeNode pageTree = new DefaultMutableTreeNode(mpc);
				cellTree.add(pageTree);
			}
		}
		if (cell.getNumVersions() > 1)
		{
			for(Iterator vIt = cell.getVersions(); vIt.hasNext(); )
			{
				Cell oldVersion = (Cell)vIt.next();
				if (oldVersion == cell) continue;
				DefaultMutableTreeNode oldCellTree = new DefaultMutableTreeNode(oldVersion);
				cellTree.add(oldCellTree);
				if (oldVersion.isMultiPage())
				{
					int pageCount = oldVersion.getNumMultiPages();
					for(int i=0; i<pageCount; i++)
					{
						MultiPageCell mpc = new MultiPageCell();
						mpc.cell = oldVersion;
						mpc.pageNo = i;
						DefaultMutableTreeNode pageTree = new DefaultMutableTreeNode(mpc);
						oldCellTree.add(pageTree);
					}
				}
			}
		}
	}

	public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf,
		int row, boolean hasFocus)
	{
		Object nodeInfo = ((DefaultMutableTreeNode)value).getUserObject();
		if (nodeInfo instanceof Cell)
		{
			Cell cell = (Cell)nodeInfo;
			if (cell.isSchematic())
			{
				Cell.CellGroup group = cell.getCellGroup();
				Cell mainSchematic = group.getMainSchematics();
				int numSchematics = 0;
				for(Iterator gIt = group.getCells(); gIt.hasNext(); )
				{
					Cell cellInGroup = (Cell)gIt.next();
					if (cellInGroup.isSchematic()) numSchematics++;
				}
				if (numSchematics > 1 && cell == mainSchematic)
					return cell.noLibDescribe() + " **";
			}
			return cell.noLibDescribe();
		}
		if (nodeInfo instanceof MultiPageCell)
		{
			MultiPageCell mpc = (MultiPageCell)nodeInfo;
			return mpc.cell.noLibDescribe() + " Page " + (mpc.pageNo+1);
		}
		if (nodeInfo instanceof Library)
		{
			Library lib = (Library)nodeInfo;
			String nodeName = lib.getName();
			if (lib == Library.getCurrent() && Library.getNumLibraries() > 1)
			{
				nodeName += " [Current]";
				iconLibrary = Resources.getResource(getClass(), "IconLibraryCheck.gif");
			}
			else
			{
				iconLibrary = Resources.getResource(getClass(), "IconLibrary.gif");
			}
			return nodeName;
		}
		if (nodeInfo instanceof CellAndCount)
		{
			CellAndCount cc = (CellAndCount)nodeInfo;
			return cc.getCell().noLibDescribe() + " (" + cc.getCount() + ")";
		}
		if (nodeInfo instanceof Cell.CellGroup)
		{
			Cell.CellGroup group = (Cell.CellGroup)nodeInfo;
            return group.getName();
		}
		if (nodeInfo instanceof ErrorLogger)
		{
			ErrorLogger el = (ErrorLogger)nodeInfo;
			return el.describe();
		}
        if (nodeInfo instanceof ErrorLogger.MessageLog)
        {
            ErrorLogger.MessageLog el = (ErrorLogger.MessageLog)nodeInfo;
            return el.getMessage();
        }
		if (nodeInfo instanceof Stimuli.Signal)
		{
			Stimuli.Signal sig = (Stimuli.Signal)nodeInfo;
			return sig.getSignalName();
		}
		if (nodeInfo == null) return "";
		return nodeInfo.toString();
	}

	// *********************************** DRAG AND DROP ***********************************

	/** Variables needed for DnD */
	private DragSource dragSource = null;
	private DefaultMutableTreeNode selectedNode;

	private void initDND()
	{
		dragSource = DragSource.getDefaultDragSource();
		DragGestureRecognizer dgr = dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_LINK, this);
	}

	public void dragGestureRecognized(DragGestureEvent e)
	{
		if (selectedNode == null) return;
		if (selectedNode.getUserObject() instanceof Stimuli.Signal)
		{
			// Get the Transferable Object
			Stimuli.Signal sSig = (Stimuli.Signal)selectedNode.getUserObject();
			Transferable transferable = new StringSelection(sSig.getFullName());

			// begin the drag
			dragSource.startDrag(e, DragSource.DefaultLinkDrop, transferable, this);
		}

		// Drag cell name to edit window
		if (selectedNode.getUserObject() instanceof Cell)
		{
			// make a Transferable Object
			Cell cell = (Cell)selectedNode.getUserObject();
			EditWindow.NodeProtoTransferable transferable = new EditWindow.NodeProtoTransferable(cell);

			// begin the drag
			dragSource.startDrag(e, DragSource.DefaultLinkDrop, transferable, this);
		}
	}

	public void dragEnter(DragSourceDragEvent e) {}

	public void dragOver(DragSourceDragEvent e) {}

	public void dragExit(DragSourceEvent e) {}

	public void dragDropEnd(DragSourceDropEvent e) {}

	public void dropActionChanged (DragSourceDragEvent e) {}


	private class MyRenderer extends DefaultTreeCellRenderer
	{
		public MyRenderer()
		{
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
			boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			// setIcon(icon)
			//setToolTipText(value.toString());
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object nodeInfo = node.getUserObject();
			if (nodeInfo instanceof Library)
			{
				if (iconLibrary == null)
					iconLibrary = Resources.getResource(getClass(), "IconLibrary.gif");
				setIcon(iconLibrary);
			}
			if (nodeInfo instanceof CellAndCount)
			{
				CellAndCount cc = (CellAndCount)nodeInfo;
				nodeInfo = cc.getCell();
			}
			if (nodeInfo instanceof Cell)
			{
				Cell cell = (Cell)nodeInfo;
				if (cell.isIcon())
				{
					if (iconViewIcon == null)
						iconViewIcon = Resources.getResource(getClass(), "IconViewIcon.gif");
					if (iconViewOldIcon == null)
						iconViewOldIcon = Resources.getResource(getClass(), "IconViewOldIcon.gif");
					if (cell.getNewestVersion() == cell) setIcon(iconViewIcon); else
						setIcon(iconViewOldIcon);
				} else if (cell.getView() == View.LAYOUT)
				{
					if (iconViewLayout == null)
						iconViewLayout = Resources.getResource(getClass(), "IconViewLayout.gif");
					if (iconViewOldLayout == null)
						iconViewOldLayout = Resources.getResource(getClass(), "IconViewOldLayout.gif");
					if (cell.getNewestVersion() == cell) setIcon(iconViewLayout); else
						setIcon(iconViewOldLayout);
				} else if (cell.isSchematic())
				{
					if (iconViewSchematics == null)
						iconViewSchematics = Resources.getResource(getClass(), "IconViewSchematics.gif");
					if (iconViewOldSchematics == null)
						iconViewOldSchematics = Resources.getResource(getClass(), "IconViewOldSchematics.gif");
					if (cell.getNewestVersion() == cell) setIcon(iconViewSchematics); else
						setIcon(iconViewOldSchematics);
				} else if (cell.getView().isTextView())
				{
					if (iconViewText == null)
						iconViewText = Resources.getResource(getClass(), "IconViewText.gif");
					if (iconViewOldText == null)
						iconViewOldText = Resources.getResource(getClass(), "IconViewOldText.gif");
					if (cell.getNewestVersion() == cell) setIcon(iconViewText); else
						setIcon(iconViewOldText);
				} else
				{
					if (iconViewMisc == null)
						iconViewMisc = Resources.getResource(getClass(), "IconViewMisc.gif");
					if (iconViewOldMisc == null)
						iconViewOldMisc = Resources.getResource(getClass(), "IconViewOldMisc.gif");
					if (cell.getNewestVersion() == cell) setIcon(iconViewMisc); else
						setIcon(iconViewOldMisc);
				}
			}
			if (nodeInfo instanceof MultiPageCell)
			{
				if (iconViewMultiPageSchematics == null)
					iconViewMultiPageSchematics = Resources.getResource(getClass(), "IconViewMultiPageSchematics.gif");
				setIcon(iconViewMultiPageSchematics);
			}
			if (nodeInfo instanceof Cell.CellGroup)
			{
				if (iconGroup == null)
					iconGroup = Resources.getResource(getClass(), "IconGroup.gif");
				setIcon(iconGroup);
			}
			if (nodeInfo instanceof String)
			{
				String theString = (String)nodeInfo;
				if (theString.equalsIgnoreCase("jobs"))
				{
					if (iconJobs == null)
						iconJobs = Resources.getResource(getClass(), "IconJobs.gif");
					setIcon(iconJobs);
				} else if (theString.equalsIgnoreCase("libraries"))
				{
					if (iconLibraries == null)
						iconLibraries = Resources.getResource(getClass(), "IconLibraries.gif");
					setIcon(iconLibraries);
				} else if (theString.equalsIgnoreCase("errors"))
				{
					if (iconErrors == null)
						iconErrors = Resources.getResource(getClass(), "IconErrors.gif");
					setIcon(iconErrors);
				} else if (theString.equalsIgnoreCase("signals"))
				{
					if (iconSignals == null)
						iconSignals = Resources.getResource(getClass(), "IconSignals.gif");
					setIcon(iconSignals);
				} else if (theString.equalsIgnoreCase("sweeps"))
				{
					if (iconSweeps == null)
						iconSweeps = Resources.getResource(getClass(), "IconSweeps.gif");
					setIcon(iconSweeps);
				}
			}
            if (nodeInfo instanceof ErrorLogger.MessageLog)
            {
                ErrorLogger.MessageLog theLog = (ErrorLogger.MessageLog)nodeInfo;
                // Error   WarningLog
                if (theLog instanceof ErrorLogger.WarningLog)
                {
                    if (iconWarnMsg == null)
                        iconWarnMsg = Resources.getResource(getClass(), "IconWarningLog.gif");
                    setIcon(iconWarnMsg);
                } else // warning
                {
                    if (iconErrorMsg == null)
                        iconErrorMsg = Resources.getResource(getClass(), "IconErrorLog.gif");
                    setIcon(iconErrorMsg);
                }
            }
			if (nodeInfo instanceof Job)
			{
				Job j = (Job)nodeInfo;
				//setToolTipText(j.getToolTip());
				//System.out.println("set tool tip to "+j.getToolTip());
			}
			return this;
		}
	}

	private static class TreeHandler implements MouseListener, MouseMotionListener
	{
		private ExplorerTree tree;
		private Object currentSelectedObject;
		private Cell originalCell;
		private boolean draggingCell;
		private MouseEvent currentMouseEvent;
		private TreePath currentPath;
		private TreePath originalPath;

		TreeHandler(ExplorerTree tree) { this.tree = tree;}

		public void mouseClicked(MouseEvent e) {}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

		public void mouseMoved(MouseEvent e) {}

		public void mousePressed(MouseEvent e)
		{
			draggingCell = false;

			// popup menu event (right click)
			if (e.isPopupTrigger())
			{
                cacheEvent(e);
				doContextMenu();
				return;
			}

            cacheEvent(e);
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();

			// double click
			if (e.getClickCount() == 2)
			{
				if (currentSelectedObject instanceof CellAndCount)
				{
					CellAndCount cc = (CellAndCount)currentSelectedObject;
					wf.setCellWindow(cc.getCell());
					return;
				}

				if (currentSelectedObject instanceof Cell)
				{
					Cell cell = (Cell)currentSelectedObject;
					wf.setCellWindow(cell);
					return;
				}
				if (currentSelectedObject instanceof MultiPageCell)
				{
					MultiPageCell mpc = (MultiPageCell)currentSelectedObject;
					Cell cell = mpc.cell;
					wf.setCellWindow(cell);
					if (wf.getContent() instanceof EditWindow)
					{
						EditWindow wnd = (EditWindow)wf.getContent();
						wnd.setMultiPageNumber(mpc.pageNo);
					}
					return;
				}

				if (currentSelectedObject instanceof Library || currentSelectedObject instanceof Cell.CellGroup ||
					currentSelectedObject instanceof String)
				{
					if (tree.isExpanded(currentPath)) tree.collapsePath(currentPath); else
						tree.expandPath(currentPath);
					return;
				}

				if (currentSelectedObject instanceof Stimuli.Signal)
				{
					Stimuli.Signal sig = (Stimuli.Signal)currentSelectedObject;
					if (wf.getContent() instanceof WaveformWindow)
					{
						WaveformWindow ww = (WaveformWindow)wf.getContent();
						ww.addSignal(sig);
					}
					return;
				}

				if (currentSelectedObject instanceof WaveformWindow.SweepSignal)
				{
					WaveformWindow.SweepSignal ss = (WaveformWindow.SweepSignal)currentSelectedObject;
					if (ss == null) return;
					ss.setIncluded(!ss.isIncluded());
					return;
				}

				if (currentSelectedObject instanceof Job)
				{
					Job job = (Job)currentSelectedObject;
					System.out.println(job.getInfo());
					return;
				}

				if (currentSelectedObject instanceof ErrorLogger.MessageLog)
				{
					ErrorLogger.MessageLog el = (ErrorLogger.MessageLog)currentSelectedObject;
					String msg = el.reportLog(true, null);
					System.out.println(msg);
					return;
				}

				// dragging: remember original object
				if (currentSelectedObject instanceof Cell)
				{
					Cell cell = (Cell)currentSelectedObject;
					if (cell.getNewestVersion() == cell)
					{
						originalCell = cell;
						originalPath = new TreePath(currentPath.getPath());
						draggingCell = true;
					}
				}
			}
		}

		public void mouseReleased(MouseEvent e)
		{
            // popup menu event (right click)
            if (e.isPopupTrigger())
            {
                cacheEvent(e);
                doContextMenu();
            }
		}

		public void mouseDragged(MouseEvent e)
		{
			if (!draggingCell) return;
			cacheEvent(e);
			tree.clearSelection();
			tree.addSelectionPath(originalPath);
			tree.addSelectionPath(currentPath);
			tree.updateUI();
		}

		private void cacheEvent(MouseEvent e)
		{
			currentPath = tree.getPathForLocation(e.getX(), e.getY());
			if (currentPath == null) { currentSelectedObject = null;   return; }
			tree.setSelectionPath(currentPath);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)currentPath.getLastPathComponent();
			tree.selectedNode = node;
			currentSelectedObject = node.getUserObject();

			// determine the source of this event
			currentMouseEvent = e;
		}

		private void doContextMenu()
		{
			// show Job menu if user clicked on a Job
			if (currentSelectedObject instanceof Job)
			{
				Job job = (Job)currentSelectedObject;
				JPopupMenu popup = job.getPopupStatus();
				popup.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (currentSelectedObject instanceof CellAndCount)
			{
				CellAndCount cc = (CellAndCount)currentSelectedObject;
				currentSelectedObject = cc.getCell();
			}
			if (currentSelectedObject instanceof Cell)
			{
				Cell cell = (Cell)currentSelectedObject;
				JPopupMenu menu = new JPopupMenu("Cell");

				JMenuItem menuItem = new JMenuItem("Edit");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(false); } });

				menuItem = new JMenuItem("Edit in New Window");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(true); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Place Instance of Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellInstanceAction(); } });

				menuItem = new JMenuItem("Create New Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Create New Version of Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellVersionAction(); } });

				menuItem = new JMenuItem("Duplicate Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { duplicateCellAction(); } });

				menuItem = new JMenuItem("Delete Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteCellAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Rename Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameCellAction(); } });

				JMenu subMenu = new JMenu("Change View");
				menu.add(subMenu);
				for(Iterator it = View.getOrderedViews().iterator(); it.hasNext(); )
				{
					View view = (View)it.next();
					if (cell.getView() == view) continue;
					JMenuItem subMenuItem = new JMenuItem(view.getFullName());
					subMenu.add(subMenuItem);
					subMenuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { reViewCellAction(e); } });
				}

                menu.addSeparator();

				if (cell.isSchematic() && cell.getNewestVersion() == cell &&
					cell.getCellGroup().getMainSchematics() != cell)
				{
					menuItem = new JMenuItem("Make This the Main Schematic");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeCellMainSchematic(); }});
				}

                menuItem = new JMenuItem("Change Cell Group...");
                menu.add(menuItem);
                menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { changeCellGroupAction(); }});

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (currentSelectedObject instanceof MultiPageCell)
			{
				MultiPageCell mpc = (MultiPageCell)currentSelectedObject;
				Cell cell = mpc.cell;
				JPopupMenu menu = new JPopupMenu("Cell");

				JMenuItem menuItem = new JMenuItem("Edit");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(false); } });

				menuItem = new JMenuItem("Edit in New Window");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(true); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Make New Page");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { makeNewSchematicPage(); } });

				menuItem = new JMenuItem("Delete This Page");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteSchematicPage(); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (currentSelectedObject instanceof Library)
			{
				Library lib = (Library)currentSelectedObject;
				JPopupMenu menu = new JPopupMenu("Library");

				JMenuItem menuItem = new JMenuItem("Open");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { openAction(); } });

				menuItem = new JMenuItem("Open all below here");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveOpenAction(); } });

				menuItem = new JMenuItem("Close all below here");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveCloseAction(); } });

				if (lib != Library.getCurrent())
				{
					menu.addSeparator();

					menuItem = new JMenuItem("Make This the Current Library");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setCurLibAction(); } });
				}

				menu.addSeparator();

				menuItem = new JMenuItem("Create New Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Rename Library");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameLibraryAction(); } });

                menuItem = new JMenuItem("Save Library");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { saveLibraryAction(); } });

				menuItem = new JMenuItem("Close Library");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { closeLibraryAction(); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (currentSelectedObject instanceof Cell.CellGroup)
			{
				JPopupMenu menu = new JPopupMenu("CellGroup");

				JMenuItem menuItem = new JMenuItem("Open");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { openAction(); } });

				menuItem = new JMenuItem("Open all below here");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveOpenAction(); } });

				menuItem = new JMenuItem("Close all below here");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveCloseAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Create New Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

				menuItem = new JMenuItem("Rename Cells in Group");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameGroupAction(); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (currentSelectedObject instanceof WaveformWindow.SweepSignal)
			{
				JPopupMenu menu = new JPopupMenu("Sweep Signal");

				JMenuItem menuItem = new JMenuItem("Include");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setSweepAction(true); } });

				menuItem = new JMenuItem("Exclude");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setSweepAction(false); } });

				menuItem = new JMenuItem("Highlight");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { highlightSweepAction(); } });

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (currentSelectedObject instanceof String)
			{
				String msg = (String)currentSelectedObject;
				if (msg.equalsIgnoreCase("sweeps"))
				{
					JPopupMenu menu = new JPopupMenu("All Sweeps");

					JMenuItem menuItem = new JMenuItem("Include All");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setAllSweepsAction(true); } });

					menuItem = new JMenuItem("Exclude All");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { setAllSweepsAction(false); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
				if (msg.equalsIgnoreCase("libraries"))
				{
					JPopupMenu menu = new JPopupMenu("Libraries");

					JMenuItem menuItem = new JMenuItem("Open");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { openAction(); } });

					menuItem = new JMenuItem("Open all below here");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveOpenAction(); } });

					menuItem = new JMenuItem("Close all below here");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { recursiveCloseAction(); } });

					menu.addSeparator();

					menuItem = new JMenuItem("Create New Cell");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

					menu.addSeparator();

					menuItem = new JMenuItem("Show Cells Alphabetically");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { showAlphabeticallyAction(); } });

					menuItem = new JMenuItem("Show Cells by Group");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { showByGroupAction(); } });

					menuItem = new JMenuItem("Show Cells by Hierarchy");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { showByHierarchyAction(); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
			}
            if (currentSelectedObject instanceof ErrorLogger) {
                ErrorLogger logger = (ErrorLogger)currentSelectedObject;
                JPopupMenu p = logger.getPopupMenu();
                if (p != null) p.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
                return;
            }
		}

		private void openAction()
		{
			tree.expandPath(currentPath);
		}

		private void recursiveOpenAction()
		{
			recursivelyOpen(currentPath);
		}

		private void recursivelyOpen(TreePath path)
		{
			tree.expandPath(path);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			int numChildren = node.getChildCount();
			for(int i=0; i<numChildren; i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
				TreePath descentPath = path.pathByAddingChild(child);
				recursivelyOpen(descentPath);
			}
		}

		private void recursiveCloseAction()
		{
			recursivelyClose(currentPath);
		}

		private void recursivelyClose(TreePath path)
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
			int numChildren = node.getChildCount();
			for(int i=0; i<numChildren; i++)
			{
				DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
				TreePath descentPath = path.pathByAddingChild(child);
				recursivelyClose(descentPath);
			}
			tree.collapsePath(path);
		}

		private void setCurLibAction()
		{
			Library lib = (Library)currentSelectedObject;
			lib.setCurrent();
			WindowFrame.wantToRedoTitleNames();
			EditWindow.repaintAll();
		}

		private void renameLibraryAction()
		{
			Library lib = (Library)currentSelectedObject;
			CircuitChanges.renameLibrary(lib);
		}

        private void saveLibraryAction()
		{
			Library lib = (Library)currentSelectedObject;
			FileMenu.saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true);
		}

		private void closeLibraryAction()
		{
			Library lib = (Library)currentSelectedObject;
			FileMenu.closeLibraryCommand(lib);
		}

		private void renameGroupAction()
		{
			Cell.CellGroup cellGroup = (Cell.CellGroup)currentSelectedObject;
			String defaultName = "";
			if (cellGroup.getNumCells() > 0)
				defaultName = ((Cell)cellGroup.getCells().next()).getName();
		
			String response = JOptionPane.showInputDialog(tree, "New name for cells in this group", defaultName);
			if (response == null) return;
			CircuitChanges.renameCellGroupInJob(cellGroup, response);
		}

		private void editCellAction(boolean newWindow)
		{
			Cell cell = null;
			int pageNo = 1;
			if (currentSelectedObject instanceof Cell)
			{
				cell = (Cell)currentSelectedObject;
			} else if (currentSelectedObject instanceof MultiPageCell)
			{
				MultiPageCell mpc = (MultiPageCell)currentSelectedObject;
				cell = mpc.cell;
				pageNo = mpc.pageNo;
			}
			WindowFrame wf = null;
 			if (newWindow)
			{
				wf = WindowFrame.createEditWindow(cell);
			} else
			{
				wf = WindowFrame.getCurrentWindowFrame();
				wf.setCellWindow(cell);
			}
			if (cell.isMultiPage() && wf.getContent() instanceof EditWindow)
			{
				EditWindow wnd = (EditWindow)wf.getContent();
				wnd.setMultiPageNumber(pageNo);
			}
		}

		private void newCellInstanceAction()
		{
			Cell cell = (Cell)currentSelectedObject;
			if (cell == null) return;
			PaletteFrame.placeInstance(cell, null, false);
		}

		private void newCellAction()
		{
			JFrame jf = TopLevel.getCurrentJFrame();
			NewCell dialog = new NewCell(jf, true);
			if (!Main.BATCHMODE) dialog.setVisible(true);
		}

		private void setSweepAction(boolean include)
		{
			WaveformWindow.SweepSignal ss = (WaveformWindow.SweepSignal)currentSelectedObject;
			if (ss == null) return;
			ss.setIncluded(include);
		}

		private void highlightSweepAction()
		{
			WaveformWindow.SweepSignal ss = (WaveformWindow.SweepSignal)currentSelectedObject;
			if (ss == null) return;
			ss.highlight();
		}

		private void setAllSweepsAction(boolean include)
		{
			WindowFrame wf = WindowFrame.getCurrentWindowFrame();
			if (wf == null) return;
			if (wf.getContent() instanceof WaveformWindow)
			{
				WaveformWindow ww = (WaveformWindow)wf.getContent();
				List sweeps = ww.getSweepSignals();
				for(Iterator it = sweeps.iterator(); it.hasNext(); )
				{
					WaveformWindow.SweepSignal ss = (WaveformWindow.SweepSignal)it.next();
					ss.setIncluded(include);
				}
			}
		}

		private void newCellVersionAction()
		{
			Cell cell = (Cell)currentSelectedObject;
			CircuitChanges.newVersionOfCell(cell);
		}

		private void duplicateCellAction()
		{
			Cell cell = (Cell)currentSelectedObject;

			String newName = JOptionPane.showInputDialog(tree, "Name of duplicated cell",
				cell.getName() + "NEW");
			if (newName == null) return;
			CircuitChanges.duplicateCell(cell, newName);
		}

		private void deleteCellAction()
		{
			Cell cell = (Cell)currentSelectedObject;
			CircuitChanges.deleteCell(cell, true);
		}

		private void renameCellAction()
		{
			Cell cell = (Cell)currentSelectedObject;
			String response = JOptionPane.showInputDialog(tree, "New name for cell " + cell.describe(), cell.getName());
			if (response == null) return;
			CircuitChanges.renameCellInJob(cell, response);
		}

		private void reViewCellAction(ActionEvent e)
		{
			JMenuItem menuItem = (JMenuItem)e.getSource();
			String viewName = menuItem.getText();
			View newView = View.findView(viewName);
			if (newView != null)
			{
				Cell cell = (Cell)currentSelectedObject;
				ViewChanges.changeCellView(cell, newView);
			}
		}

		private void makeCellMainSchematic()
		{
            Cell cell = (Cell)currentSelectedObject;
            if (cell == null) return;
            cell.getCellGroup().setMainSchematics(cell);
		}

        private void changeCellGroupAction() {
            Cell cell = (Cell)currentSelectedObject;
            if (cell == null) return;
            ChangeCellGroup dialog = new ChangeCellGroup(TopLevel.getCurrentJFrame(), true, cell, cell.getLibrary());
            if (!Main.BATCHMODE) dialog.setVisible(true);
        }

        private void makeNewSchematicPage()
        {
        	MultiPageCell mpc = (MultiPageCell)currentSelectedObject;
            Cell cell = mpc.cell;
         	if (!cell.isMultiPage())
        	{
        		System.out.println("First turn this cell into a multi-page schematic");
        		return;
        	}
        	int numPages = cell.getNumMultiPages();
    		CellMenu.SetMultiPageJob job = new CellMenu.SetMultiPageJob(cell, numPages+1);
           	EditWindow wnd = EditWindow.needCurrent();
        	if (wnd != null) wnd.setMultiPageNumber(numPages);
        }

        private void deleteSchematicPage()
        {
        	MultiPageCell mpc = (MultiPageCell)currentSelectedObject;
            Cell cell = mpc.cell;
         	if (!cell.isMultiPage()) return;
        	int numPages = cell.getNumMultiPages();
         	if (numPages <= 1)
        	{
        		System.out.println("Cannot delete the last page of a multi-page schematic");
        		return;
        	}
         	CellMenu.DeleteMultiPageJob job = new CellMenu.DeleteMultiPageJob(cell, mpc.pageNo);
        }

        private void showAlphabeticallyAction()
		{
			howToShow = SHOWALPHABETICALLY;
			WindowFrame.wantToRedoLibraryTree();
		}

		private void showByGroupAction()
		{
			howToShow = SHOWBYCELLGROUP;
			WindowFrame.wantToRedoLibraryTree();
		}

		private void showByHierarchyAction()
		{
			howToShow = SHOWBYHIERARCHY;
			WindowFrame.wantToRedoLibraryTree();
		}

	}
}
