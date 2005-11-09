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
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.project.Project;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Resources;
import com.sun.electric.tool.user.ViewChanges;
import com.sun.electric.tool.user.dialogs.ChangeCellGroup;
import com.sun.electric.tool.user.dialogs.NewCell;
import com.sun.electric.tool.user.menus.FileMenu;
import com.sun.electric.tool.user.menus.CellMenu;
import com.sun.electric.tool.user.tecEdit.Manipulate;
import com.sun.electric.tool.user.tecEdit.ArcInfo;
import com.sun.electric.tool.user.tecEdit.LayerInfo;
import com.sun.electric.tool.user.tecEdit.NodeInfo;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.awt.datatransfer.StringSelection;
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
	private Object currentSelectedObject;

	private static final int SHOWALPHABETICALLY = 1;
	private static final int SHOWBYCELLGROUP    = 2;
	private static final int SHOWBYHIERARCHY    = 3;
	private static int howToShow = SHOWBYCELLGROUP;

	private static class IconGroup
	{
		/** the icon for a normal cell */					private ImageIcon regular;
		/** the icon for an old version of a cell */		private ImageIcon old;
		/** the icon for a checked-in cell */				private ImageIcon available;
		/** the icon for a cell checked-out to others */	private ImageIcon locked;
		/** the icon for a cell checked-out to you */		private ImageIcon unlocked;
	}
	private static ImageIcon iconLibrary = null;
	private static ImageIcon iconGroup = null;
	private static ImageIcon iconJobs = null;
	private static ImageIcon iconLibraries = null;
	private static ImageIcon iconErrors = null;
	private static ImageIcon iconErrorMsg = null;
    private static ImageIcon iconWarnMsg = null;
	private static ImageIcon iconSignals = null;
	private static ImageIcon iconSweeps = null;
//	private static ImageIcon iconViewIcon = null;
//	private static ImageIcon iconViewLayout = null;
	private static ImageIcon iconViewMultiPageSchematics = null;
//	private static ImageIcon iconViewSchematics = null;
//	private static ImageIcon iconViewMisc = null;
//	private static ImageIcon iconViewText = null;
	private static ImageIcon iconSpiderWeb = null;
	private static ImageIcon iconLocked = null;
	private static ImageIcon iconUnlocked = null;
	private static ImageIcon iconAvailable = null;

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
	public Object getCurrentlySelectedObject() { return currentSelectedObject; }

	/**
	 * Method to return the currently selected object in the explorer tree.
	 * @return the currently selected object in the explorer tree.
	 */
	public DefaultTreeModel getTreeModel() { return treeModel; }

	/**
	 * Method to set the currently selected object in the explorer tree.
	 * param obj the currently selected object in the explorer tree.
	 */
	public void setCurrentlySelectedObject(Object obj) { currentSelectedObject = obj; }

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
		for(Iterator<Library> it = Library.getVisibleLibraries().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			if (!addTechnologyLibraryToTree(lib, libTree))
			{
				for(Iterator<Cell> eit = lib.getCells(); eit.hasNext(); )
				{
					Cell cell = (Cell)eit.next();
					DefaultMutableTreeNode cellTree = new DefaultMutableTreeNode(cell);
					libTree.add(cellTree);
				}
			}
			libraryExplorerTree.add(libTree);
		}
	}

	private static boolean addTechnologyLibraryToTree(Library lib, DefaultMutableTreeNode libTree)
	{
		boolean techLib = false;
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (cell.isInTechnologyLibrary())
			{
				techLib = true;
				break;
			}
		}
		if (!techLib) return false;

		// add this library as a technology library
		DefaultMutableTreeNode layerTree = new DefaultMutableTreeNode("TECHNOLOGY LAYERS");
		DefaultMutableTreeNode arcTree = new DefaultMutableTreeNode("TECHNOLOGY ARCS");
		DefaultMutableTreeNode nodeTree = new DefaultMutableTreeNode("TECHNOLOGY NODES");
		DefaultMutableTreeNode miscTree = new DefaultMutableTreeNode("TECHNOLOGY SUPPORT");
		libTree.add(layerTree);
		libTree.add(arcTree);
		libTree.add(nodeTree);
		libTree.add(miscTree);
		HashSet<Cell> allCells = new HashSet<Cell>();
		Cell [] layerCells = LayerInfo.getLayerCells(lib);
		for(int i=0; i<layerCells.length; i++)
		{
			allCells.add(layerCells[i]);
			layerTree.add(new DefaultMutableTreeNode(layerCells[i]));
		}
		Cell [] arcCells = ArcInfo.getArcCells(lib);
		for(int i=0; i<arcCells.length; i++)
		{
			allCells.add(arcCells[i]);
			arcTree.add(new DefaultMutableTreeNode(arcCells[i]));
		}
		Cell [] nodeCells = NodeInfo.getNodeCells(lib);
		for(int i=0; i<nodeCells.length; i++)
		{
			allCells.add(nodeCells[i]);
			nodeTree.add(new DefaultMutableTreeNode(nodeCells[i]));
		}
		for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (allCells.contains(cell)) continue;
			miscTree.add(new DefaultMutableTreeNode(cell));
		}
		return true;
	}

	private static synchronized void rebuildExplorerTreeByHierarchy(DefaultMutableTreeNode libraryExplorerTree)
	{
		/*for(Library lib: Library.getVisibleLibraries())*/
		for(Iterator<Library> it = Library.getVisibleLibraries().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			if (!addTechnologyLibraryToTree(lib, libTree))
			{
				for(Iterator<Cell> eit = lib.getCells(); eit.hasNext(); )
				{
					Cell cell = (Cell)eit.next();
	
					// ignore icons and text views
					if (cell.isIcon()) continue;
					if (cell.getView().isTextView()) continue;
	
			        HashSet<Cell> addedCells = new HashSet<Cell>();
					for(Iterator<Cell> vIt = cell.getVersions(); vIt.hasNext(); )
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
		HashMap<Cell,DBMath.MutableInteger> cellCount = new HashMap<Cell,DBMath.MutableInteger>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
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
		for(Iterator<Cell> it = cellCount.keySet().iterator(); it.hasNext(); )
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
		HashSet<Cell> cellsSeen = new HashSet<Cell>();
		/*for(Library lib: Library.getVisibleLibraries())*/
		for(Iterator<Library> it = Library.getVisibleLibraries().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			if (!addTechnologyLibraryToTree(lib, libTree))
			{
				for(Iterator<Cell> eit = lib.getCells(); eit.hasNext(); )
				{
					Cell cell = (Cell)eit.next();
					cellsSeen.remove(cell);
				}
				for(Iterator<Cell> eit = lib.getCells(); eit.hasNext(); )
				{
					Cell cell = (Cell)eit.next();
					if (cell.getNewestVersion() != cell) continue;
					Cell.CellGroup group = cell.getCellGroup();
					int numNewCells = 0;
					for(Iterator<Cell> gIt = group.getCells(); gIt.hasNext(); )
					{
						Cell cellInGroup = (Cell)gIt.next();
						if (cellInGroup.getNewestVersion() == cellInGroup) numNewCells++;
					}
					if (numNewCells == 1)
					{
						addCellAndAllVersions(cell, libTree);
						continue;
					}
	
					List<Cell> cellsInGroup = group.getCellsSortedByView();
					DefaultMutableTreeNode groupTree = null;
					for(Iterator<Cell> gIt = cellsInGroup.iterator(); gIt.hasNext(); )
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
			for(Iterator<Cell> vIt = cell.getVersions(); vIt.hasNext(); )
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
				for(Iterator<Cell> gIt = group.getCells(); gIt.hasNext(); )
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
		if (nodeInfo instanceof Signal)
		{
			Signal sig = (Signal)nodeInfo;
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
		if (selectedNode.getUserObject() instanceof Signal)
		{
			// Get the Transferable Object
			Signal sSig = (Signal)selectedNode.getUserObject();
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
            if (TopLevel.getOperatingSystem() == TopLevel.OS.MACINTOSH)
            {
                // OS X has problems creating DefaultDragImage
                Image img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
			    dragSource.startDrag(e, DragSource.DefaultLinkDrop, img, new Point(0,0), transferable, this);
            }
            else
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
		private Font plainFont, boldFont, italicFont;

		public MyRenderer()
		{
			plainFont = new Font("arial", Font.PLAIN, 11);
			boldFont = new Font("arial", Font.BOLD, 11);
            italicFont = new Font("arial", Font.ITALIC, 11);
		}

		public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
			boolean expanded, boolean leaf, int row, boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
			// setIcon(icon)
			//setToolTipText(value.toString());
			setFont(plainFont);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object nodeInfo = node.getUserObject();
			if (nodeInfo instanceof Library)
			{
				Library lib = (Library)nodeInfo;
				if (iconLibrary == null)
					iconLibrary = Resources.getResource(getClass(), "IconLibrary.gif");
				if (lib.isChangedMajor()) setFont(boldFont);
                else if (lib.isChangedMinor()) setFont(italicFont);
				setIcon(iconLibrary);
			}
			if (nodeInfo instanceof CellAndCount)
			{
				CellAndCount cc = (CellAndCount)nodeInfo;
				nodeInfo = cc.getCell();
                if (cc.getCell().isModified(true)) setFont(boldFont);
                else if (cc.getCell().isModified(false)) setFont(italicFont);
			}
			if (nodeInfo instanceof Cell)
			{
				Cell cell = (Cell)nodeInfo;
                if (cell.isModified(true)) setFont(boldFont);
                else if (cell.isModified(false)) setFont(italicFont);
				IconGroup ig;
				if (cell.isIcon()) ig = findIconGroup(View.ICON); else
					if (cell.getView() == View.LAYOUT) ig = findIconGroup(View.LAYOUT); else
						if (cell.isSchematic()) ig = findIconGroup(View.SCHEMATIC); else
							if (cell.getView().isTextView()) ig = findIconGroup(View.DOC); else
								ig = findIconGroup(View.UNKNOWN);
				if (cell.getNewestVersion() != cell) setIcon(ig.old); else
				{
					switch (Project.getCellStatus(cell))
					{
						case Project.NOTMANAGED:         setIcon(ig.regular);     break;
						case Project.CHECKEDIN:          setIcon(ig.available);   break;
						case Project.CHECKEDOUTTOOTHERS: setIcon(ig.locked);      break;
						case Project.CHECKEDOUTTOYOU:    setIcon(ig.unlocked);    break;
					}
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
                Cell.CellGroup cg = (Cell.CellGroup)nodeInfo;
                int status = -1; // hasn't changed , status = 1 -> major change, status = 0 -> minor change
                for (Iterator<Cell> it = cg.getCells(); status != 1 && it.hasNext();)
                {
                    Cell c = (Cell) it.next();
                    if (c.isModified(true))
                    {
                        status = 1;
                        break;  // no need of checking the rest
                    }
                    else if (c.isModified(false)) status = 0;
                }
                if (status == 1) setFont(boldFont);
                else if (status == 0) setFont(italicFont);
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

		private HashMap<View,IconGroup> iconGroups = new HashMap<View,IconGroup>();

		private IconGroup findIconGroup(View view)
		{
			IconGroup ig = (IconGroup)iconGroups.get(view);
			if (ig == null)
			{
				ig = new IconGroup();

				// get the appropriate background icon
				if (view == View.LAYOUT) ig.regular = Resources.getResource(getClass(), "IconViewLayout.gif"); else
				if (view == View.SCHEMATIC) ig.regular = Resources.getResource(getClass(), "IconViewSchematics.gif"); else
				if (view == View.ICON) ig.regular = Resources.getResource(getClass(), "IconViewIcon.gif"); else
				if (view == View.DOC) ig.regular = Resources.getResource(getClass(), "IconViewText.gif"); else
				ig.regular = Resources.getResource(getClass(), "IconViewMisc.gif");

				// make sure the overlay icons have been read
				if (iconSpiderWeb == null) iconSpiderWeb = Resources.getResource(getClass(), "IconSpiderWeb.gif");
				if (iconLocked == null) iconLocked = Resources.getResource(getClass(), "IconLocked.gif");
				if (iconUnlocked == null) iconUnlocked = Resources.getResource(getClass(), "IconUnlocked.gif");
				if (iconAvailable == null) iconAvailable = Resources.getResource(getClass(), "IconAvailable.gif");

				ig.old = buildIcon(iconSpiderWeb, ig.regular);
				ig.available = buildIcon(iconAvailable, ig.regular);
				ig.locked = buildIcon(iconLocked, ig.regular);
				ig.unlocked = buildIcon(iconUnlocked, ig.regular);
				iconGroups.put(view, ig);
			}
			return ig;
		}

		private ImageIcon buildIcon(ImageIcon fg, ImageIcon bg)
		{
			// overlay and create the other icons for this view
			int wid = fg.getIconWidth();
			int hei = fg.getIconHeight();
			BufferedImage bi = new BufferedImage(wid, hei, BufferedImage.TYPE_INT_RGB);

			int [] backgroundValues = new int[wid*hei];
			PixelGrabber background = new PixelGrabber(bg.getImage(), 0, 0, wid, hei, backgroundValues, 0, wid);
			int [] foregroundValues = new int[wid*hei];
			PixelGrabber foreground = new PixelGrabber(fg.getImage(), 0, 0, wid, hei, foregroundValues, 0, wid);
			try
			{
				background.grabPixels();
				foreground.grabPixels();
			} catch (InterruptedException e) {}
			for(int y=0; y<hei; y++)
			{
				for(int x=0; x<wid; x++)
				{
					int bCol = backgroundValues[y*wid+x];
					int fCol = foregroundValues[y*wid+x];
					if ((fCol&0xFFFFFF) != 0xFFFFFF) bCol = fCol;
					bi.setRGB(x, y, bCol);
				}
			}
			return new ImageIcon(bi);
		}
	}

	private static class TreeHandler implements MouseListener, MouseMotionListener
	{
		private ExplorerTree tree;
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
				if (tree.currentSelectedObject instanceof CellAndCount)
				{
					CellAndCount cc = (CellAndCount)tree.currentSelectedObject;
					wf.setCellWindow(cc.getCell());
					return;
				}

				if (tree.currentSelectedObject instanceof Cell)
				{
					Cell cell = (Cell)tree.currentSelectedObject;
					wf.setCellWindow(cell);
					return;
				}
				if (tree.currentSelectedObject instanceof MultiPageCell)
				{
					MultiPageCell mpc = (MultiPageCell)tree.currentSelectedObject;
					Cell cell = mpc.cell;
					wf.setCellWindow(cell);
					if (wf.getContent() instanceof EditWindow)
					{
						EditWindow wnd = (EditWindow)wf.getContent();
						wnd.setMultiPageNumber(mpc.pageNo);
					}
					return;
				}

				if (tree.currentSelectedObject instanceof Library || tree.currentSelectedObject instanceof Cell.CellGroup ||
						tree.currentSelectedObject instanceof String)
				{
					if (tree.isExpanded(currentPath)) tree.collapsePath(currentPath); else
						tree.expandPath(currentPath);
					return;
				}

				if (tree.currentSelectedObject instanceof Signal)
				{
					Signal sig = (Signal)tree.currentSelectedObject;
					if (wf.getContent() instanceof WaveformWindow)
					{
						WaveformWindow ww = (WaveformWindow)wf.getContent();
						ww.addSignal(sig);
					}
					return;
				}

				if (tree.currentSelectedObject instanceof WaveformWindow.SweepSignal)
				{
					WaveformWindow.SweepSignal ss = (WaveformWindow.SweepSignal)tree.currentSelectedObject;
					if (ss == null) return;
					ss.setIncluded(!ss.isIncluded());
					return;
				}

				if (tree.currentSelectedObject instanceof Job)
				{
					Job job = (Job)tree.currentSelectedObject;
					System.out.println(job.getInfo());
					return;
				}

				if (tree.currentSelectedObject instanceof ErrorLogger.MessageLog)
				{
					ErrorLogger.MessageLog el = (ErrorLogger.MessageLog)tree.currentSelectedObject;
					String msg = el.reportLog(true, null);
					System.out.println(msg);
					return;
				}

				// dragging: remember original object
				if (tree.currentSelectedObject instanceof Cell)
				{
					Cell cell = (Cell)tree.currentSelectedObject;
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
			if (currentPath == null) { tree.currentSelectedObject = null;   return; }
			tree.setSelectionPath(currentPath);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)currentPath.getLastPathComponent();
			tree.selectedNode = node;
			Object newSelection = node.getUserObject();
			if (newSelection != tree.currentSelectedObject)
			{
				tree.currentSelectedObject = newSelection;

				// update highlighting to match this selection
				for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
				{
					WindowFrame wf = (WindowFrame)it.next();
					if (wf.getExplorerTab() == tree)
					{
						// initiate crossprobing from WaveformWindow 
						if (wf.getContent() instanceof WaveformWindow)
						{
							WaveformWindow ww = (WaveformWindow)wf.getContent();
							ww.crossProbeWaveformToEditWindow();
						}
					}
				}
			}

			
			// determine the source of this event
			currentMouseEvent = e;
		}

		private void doContextMenu()
		{
			Object selectedObject = tree.currentSelectedObject;
			// show Job menu if user clicked on a Job
			if (selectedObject instanceof Job)
			{
				Job job = (Job)selectedObject;
				JPopupMenu popup = job.getPopupStatus();
				popup.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (selectedObject instanceof CellAndCount)
			{
				CellAndCount cc = (CellAndCount)selectedObject;
				selectedObject = cc.getCell();
			}
			if (selectedObject instanceof Cell)
			{
				Cell cell = (Cell)selectedObject;
				JPopupMenu menu = new JPopupMenu("Cell");

				JMenuItem menuItem = new JMenuItem("Edit");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(false); } });

				menuItem = new JMenuItem("Edit in New Window");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { editCellAction(true); } });

				int projStatus = Project.getCellStatus(cell);
				if (projStatus != Project.OLDVERSION &&
					(projStatus != Project.NOTMANAGED || Project.isLibraryManaged(cell.getLibrary())))
				{
					menu.addSeparator();

					if (projStatus == Project.CHECKEDIN)
					{
						menuItem = new JMenuItem("Check Out");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Project.checkOut((Cell)tree.currentSelectedObject); } });
					}
					if (projStatus == Project.CHECKEDOUTTOYOU)
					{
						menuItem = new JMenuItem("Check In...");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Project.checkIn((Cell)tree.currentSelectedObject); } });

						menuItem = new JMenuItem("Rollback and Release Check-Out");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Project.cancelCheckOut((Cell)tree.currentSelectedObject); } });
					}
					if (projStatus == Project.NOTMANAGED)
					{
						menuItem = new JMenuItem("Add To Repository");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Project.addCell((Cell)tree.currentSelectedObject); } });
					} else
					{
						menuItem = new JMenuItem("Show History of This Cell...");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Project.examineHistory((Cell)tree.currentSelectedObject); } });
					}
					if (projStatus == Project.CHECKEDIN || projStatus == Project.CHECKEDOUTTOYOU)
					{
						menuItem = new JMenuItem("Remove From Repository");
						menu.add(menuItem);
						menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Project.removeCell((Cell)tree.currentSelectedObject); } });
					}
				}

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
				for(Iterator<View> it = View.getOrderedViews().iterator(); it.hasNext(); )
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
			if (selectedObject instanceof MultiPageCell)
			{
				MultiPageCell mpc = (MultiPageCell)selectedObject;
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
			if (selectedObject instanceof Library)
			{
				Library lib = (Library)selectedObject;
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

				if (Project.isLibraryManaged(lib))
				{
					menuItem = new JMenuItem("Update from Repository");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Project.updateProject(); } });
				} else
				{
					menuItem = new JMenuItem("Add to Project Management Repository");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Project.addALibrary((Library)tree.currentSelectedObject); } });
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
			if (selectedObject instanceof Cell.CellGroup)
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
			if (selectedObject instanceof WaveformWindow.SweepSignal)
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
			if (selectedObject instanceof String)
			{
				String msg = (String)selectedObject;
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
                if (msg.equalsIgnoreCase("errors"))
				{
					JPopupMenu menu = new JPopupMenu("Errors");

					JMenuItem menuItem = new JMenuItem("Delete All");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ErrorLogger.deleteAllLoggers(); } });

                    menuItem = new JMenuItem("Load Logger");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { ErrorLogger.load(); } });

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

					menu.addSeparator();

                    menuItem = new JMenuItem("Search");
                    menu.add(menuItem);
                    menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { searchAction(); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
				if (msg.equalsIgnoreCase("TECHNOLOGY LAYERS"))
				{
					JPopupMenu menu = new JPopupMenu("Technology Layers");

					JMenuItem menuItem = new JMenuItem("Add New Layer");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.makeCell(1); } });

					menuItem = new JMenuItem("Reorder Layers");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.reorderPrimitives(1); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
				if (msg.equalsIgnoreCase("TECHNOLOGY ARCS"))
				{
					JPopupMenu menu = new JPopupMenu("Technology Arcs");

					JMenuItem menuItem = new JMenuItem("Add New Arc");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.makeCell(2); } });

					menuItem = new JMenuItem("Reorder Arcs");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.reorderPrimitives(2); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
				if (msg.equalsIgnoreCase("TECHNOLOGY NODES"))
				{
					JPopupMenu menu = new JPopupMenu("Technology Nodes");

					JMenuItem menuItem = new JMenuItem("Add New Node");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.makeCell(3); } });

					menuItem = new JMenuItem("Reorder Nodes");
					menu.add(menuItem);
					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { Manipulate.reorderPrimitives(3); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
			}
            if (selectedObject instanceof ErrorLogger) {
                ErrorLogger logger = (ErrorLogger)selectedObject;
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
			Library lib = (Library)tree.currentSelectedObject;
			lib.setCurrent();
			WindowFrame.wantToRedoTitleNames();
            WindowFrame.wantToRedoLibraryTree();
			EditWindow.repaintAll();
		}

		private void renameLibraryAction()
		{
			Library lib = (Library)tree.currentSelectedObject;
			CircuitChanges.renameLibrary(lib);
		}

        private void saveLibraryAction()
		{
			Library lib = (Library)tree.currentSelectedObject;
			FileMenu.saveLibraryCommand(lib, FileType.DEFAULTLIB, false, true);
		}

		private void closeLibraryAction()
		{
			Library lib = (Library)tree.currentSelectedObject;
			FileMenu.closeLibraryCommand(lib);
		}

		private void renameGroupAction()
		{
			Cell.CellGroup cellGroup = (Cell.CellGroup)tree.currentSelectedObject;
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
			if (tree.currentSelectedObject instanceof Cell)
			{
				cell = (Cell)tree.currentSelectedObject;
			} else if (tree.currentSelectedObject instanceof MultiPageCell)
			{
				MultiPageCell mpc = (MultiPageCell)tree.currentSelectedObject;
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
			Cell cell = (Cell)tree.currentSelectedObject;
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
			WaveformWindow.SweepSignal ss = (WaveformWindow.SweepSignal)tree.currentSelectedObject;
			if (ss == null) return;
			ss.setIncluded(include);
		}

		private void highlightSweepAction()
		{
			WaveformWindow.SweepSignal ss = (WaveformWindow.SweepSignal)tree.currentSelectedObject;
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
				List<WaveformWindow.SweepSignal> sweeps = ww.getSweepSignals();
				for(Iterator<WaveformWindow.SweepSignal> it = sweeps.iterator(); it.hasNext(); )
				{
					WaveformWindow.SweepSignal ss = (WaveformWindow.SweepSignal)it.next();
					ss.setIncluded(include);
				}
			}
		}

		private void newCellVersionAction()
		{
			Cell cell = (Cell)tree.currentSelectedObject;
			CircuitChanges.newVersionOfCell(cell);
		}

		private void duplicateCellAction()
		{
			Cell cell = (Cell)tree.currentSelectedObject;

			String newName = JOptionPane.showInputDialog(tree, "Name of duplicated cell",
				cell.getName() + "NEW");
			if (newName == null) return;
			CircuitChanges.duplicateCell(cell, newName);
		}

		private void deleteCellAction()
		{
			Cell cell = (Cell)tree.currentSelectedObject;
			CircuitChanges.deleteCell(cell, true, false);
		}

		private void renameCellAction()
		{
			Cell cell = (Cell)tree.currentSelectedObject;
			String response = JOptionPane.showInputDialog(tree, "New name for " + cell, cell.getName());
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
				Cell cell = (Cell)tree.currentSelectedObject;
				ViewChanges.changeCellView(cell, newView);
			}
		}

		private void makeCellMainSchematic()
		{
            Cell cell = (Cell)tree.currentSelectedObject;
            if (cell == null) return;
            cell.getCellGroup().setMainSchematics(cell);
		}

        private void changeCellGroupAction() {
            Cell cell = (Cell)tree.currentSelectedObject;
            if (cell == null) return;
            ChangeCellGroup dialog = new ChangeCellGroup(TopLevel.getCurrentJFrame(), true, cell, cell.getLibrary());
            if (!Main.BATCHMODE) dialog.setVisible(true);
        }

        private void makeNewSchematicPage()
        {
        	MultiPageCell mpc = (MultiPageCell)tree.currentSelectedObject;
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
        	MultiPageCell mpc = (MultiPageCell)tree.currentSelectedObject;
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

        private void searchAction()
        {
            String name = JOptionPane.showInputDialog(tree, "Name of cell to search","");
			if (name == null) return;
            System.out.println("Searching cell name like " + name);
            Cell cell = Library.findCellInLibraries(name, null);
            if (cell != null)
                System.out.println("\t" + cell + " in " + cell.getLibrary());
        }
	}
}
