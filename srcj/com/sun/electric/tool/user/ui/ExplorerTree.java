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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.CircuitChanges;
import com.sun.electric.tool.user.dialogs.NewCell;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Component;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/**
 * Class to display a cell explorer tree-view of the database.
 */
public class ExplorerTree extends JTree 
{
	private TreeHandler handler = null;
	private DefaultMutableTreeNode rootNode;
	private DefaultTreeModel treeModel;
	private HashMap expanded;

	private static final int SHOWALPHABETICALLY = 1;
	private static final int SHOWBYCELLGROUP    = 2;
	private static final int SHOWBYHIERARCHY    = 3;
	private static int howToShow = SHOWBYCELLGROUP;

	private static ImageIcon iconLibrary = null;
	private static ImageIcon iconGroup = null;
	private static ImageIcon iconJobs = null;
	private static ImageIcon iconLibraries = null;
	private static ImageIcon iconViewIcon = null;
	private static ImageIcon iconViewOldIcon = null;
	private static ImageIcon iconViewLayout = null;
	private static ImageIcon iconViewOldLayout = null;
	private static ImageIcon iconViewSchematics = null;
	private static ImageIcon iconViewOldSchematics = null;
	private static ImageIcon iconViewMisc = null;
	private static ImageIcon iconViewOldMisc = null;
	private static ImageIcon iconViewText = null;
	private static ImageIcon iconViewOldText = null;
	private static DefaultMutableTreeNode libraryExplorerTree = new DefaultMutableTreeNode("LIBRARIES");
	private static boolean explorerChanged = true;

	/**
	 * Routine to create a new ExplorerTree.
	 * @param treeModel the tree to display.
	 * @param wnd the window associated with this tree.
	 * @return the newly created ExplorerTree.
	 */
	public static ExplorerTree CreateExplorerTree(DefaultMutableTreeNode rootNode, DefaultTreeModel treeModel, EditWindow wnd)
	{
		ExplorerTree tree = new ExplorerTree(rootNode, treeModel);
		tree.handler = new TreeHandler(tree, wnd);
		tree.addMouseListener(tree.handler);
		return tree;
	}

	private ExplorerTree(DefaultMutableTreeNode rootNode, DefaultTreeModel treeModel)
	{
		super(treeModel);
		this.rootNode = rootNode;
		this.treeModel = treeModel;

//		setEditable(true);
//		setDragEnabled(true);

		// single selection as default
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// do not show top-level
		setRootVisible(false);
		setShowsRootHandles(true);
		setToggleClickCount(3);

		// show one level of indentation
		//	collapseRow(1);

		// enable tool tips - we'll use these to display useful info
		ToolTipManager.sharedInstance().registerComponent(this);

		// register our own extended renderer for custom icons and tooltips
		setCellRenderer(new MyRenderer());

		expanded = new HashMap();
	}

	/**
	 * Routine to return the tree structure that defines the current cell explorer.
	 * This is a tree of DefaultMutableTreeNode objects that can be used to
	 * explore the cell hierarchy.
	 * @return the tree structure that defines the current cell explorer.
	 */
	public static DefaultMutableTreeNode getLibraryExplorerTree() { return libraryExplorerTree; }

	public static void explorerTreeChanged() { explorerChanged = true; }

    /**
	 * Routine called when the explorer information changes.
	 * It updates the display for minor changes.  See JTree.treeDidChange().
     */
	public static synchronized void explorerTreeMinorlyChanged()
	{
		if (explorerChanged) return;
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
            wf.getExplorerTree().treeDidChange();
		}
	}

	public static void rebuildExplorerTree()
	{
		if (!explorerChanged) return;

		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			wf.getExplorerTree().cacheExpansion();
		}

		// reconstruct the tree
		switch (howToShow)
		{
			case SHOWALPHABETICALLY:
				rebuildExplorerTreeByName();
				break;
			case SHOWBYCELLGROUP:
				rebuildExplorerTreeByGroups();
				break;
			case SHOWBYHIERARCHY:
				rebuildExplorerTreeByHierarchy();
				break;
		}

		// redisplay
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			wf.getExplorerTree().treeModel.reload();
			wf.getExplorerTree().restoreExpansion();
		}
		explorerChanged = false;
	}

	private static void rebuildExplorerTreeByName()
	{
		libraryExplorerTree.removeAllChildren();
		List sortedList = Library.getVisibleLibrariesSortedByName();
		for(Iterator it = sortedList.iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			for(Iterator eit = lib.getCellsSortedByName().iterator(); eit.hasNext(); )
			{
				Cell cell = (Cell)eit.next();
				DefaultMutableTreeNode cellTree = new DefaultMutableTreeNode(cell);
				libTree.add(cellTree);
			}
			libraryExplorerTree.add(libTree);
		}
	}

	private static void rebuildExplorerTreeByHierarchy()
	{
	}

	private static void rebuildExplorerTreeByGroups()
	{
		libraryExplorerTree.removeAllChildren();
		List sortedList = Library.getVisibleLibrariesSortedByName();

		FlagSet cellFlag = NodeProto.getFlagSet(1);
		for(Iterator it = sortedList.iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			DefaultMutableTreeNode libTree = new DefaultMutableTreeNode(lib);
			List cells = lib.getCellsSortedByName();
			for(Iterator eit = cells.iterator(); eit.hasNext(); )
			{
				Cell cell = (Cell)eit.next();
				cell.clearBit(cellFlag);
			}
			for(Iterator eit = cells.iterator(); eit.hasNext(); )
			{
				Cell cell = (Cell)eit.next();
				if (cell.getNewestVersion() != cell) continue;
				Cell.CellGroup group = cell.getCellGroup();
				if (group.getNumCells() == 1)
				{
					DefaultMutableTreeNode cellTree = new DefaultMutableTreeNode(cell);
					libTree.add(cellTree);
					if (cell.getNumVersions() > 1)
					{
						for(Iterator vIt = cell.getVersions(); vIt.hasNext(); )
						{
							Cell oldVersion = (Cell)vIt.next();
							if (oldVersion == cell) continue;
							DefaultMutableTreeNode oldCellTree = new DefaultMutableTreeNode(oldVersion);
							cellTree.add(oldCellTree);
						}
					}
					continue;
				}

				List cellsInGroup = group.getCellsSortedByView();
				DefaultMutableTreeNode groupTree = null;
				for(Iterator gIt = cellsInGroup.iterator(); gIt.hasNext(); )
				{
					Cell cellInGroup = (Cell)gIt.next();
					if (cellInGroup.isBit(cellFlag)) continue;
					if (groupTree == null)
					{
						groupTree = new DefaultMutableTreeNode(group);
					}
					DefaultMutableTreeNode cellTree = new DefaultMutableTreeNode(cellInGroup);
					groupTree.add(cellTree);
					cellInGroup.setBit(cellFlag);
					if (cellInGroup.getNumVersions() > 1)
					{
						for(Iterator vIt = cellInGroup.getVersions(); vIt.hasNext(); )
						{
							Cell oldVersion = (Cell)vIt.next();
							if (oldVersion == cellInGroup) continue;
							DefaultMutableTreeNode oldCellTree = new DefaultMutableTreeNode(oldVersion);
							cellTree.add(oldCellTree);
						}
					}
				}
				if (groupTree != null)
					libTree.add(groupTree);
			}
			libraryExplorerTree.add(libTree);
		}
		cellFlag.freeFlagSet();
	}

	public void cacheExpansion()
	{
		expanded.clear();
		recursivelyCache(new TreePath(rootNode), true);
	}

	public void restoreExpansion()
	{
		recursivelyCache(new TreePath(rootNode), false);
		expanded.clear();
	}

	private void recursivelyCache(TreePath path, boolean cache)
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		Object obj = node.getUserObject();
		int numChildren = node.getChildCount();
		if (numChildren == 0) return;

		if (cache)
		{
			if (isExpanded(path)) expanded.put(obj, obj);
		} else
		{
			if (expanded.get(obj) != null) expandPath(path);
		}

		// now recurse
		for(int i=0; i<numChildren; i++)
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)node.getChildAt(i);
			TreePath descentPath = path.pathByAddingChild(child);
			recursivelyCache(descentPath, cache);
		}
	}

	public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf,
		int row, boolean hasFocus)
	{
		Object nodeInfo = ((DefaultMutableTreeNode)value).getUserObject();
		if (nodeInfo instanceof Cell)
		{
			Cell cell = (Cell)nodeInfo;
			if (cell.getView() == View.SCHEMATIC)
			{
				Cell.CellGroup group = cell.getCellGroup();
				Cell mainSchematic = group.getMainSchematics();
				int numSchematics = 0;
				for(Iterator gIt = group.getCells(); gIt.hasNext(); )
				{
					Cell cellInGroup = (Cell)gIt.next();
					if (cellInGroup.getView() == View.SCHEMATIC) numSchematics++;
				}
				if (numSchematics > 1 && cell == mainSchematic)
					return cell.noLibDescribe() + " **";
			}
			return cell.noLibDescribe();
		}
		if (nodeInfo instanceof Library)
		{
			Library lib = (Library)nodeInfo;
			String nodeName = lib.getLibName();
			if (lib == Library.getCurrent()) nodeName += " [Current]";
			return nodeName;
		}
		if (nodeInfo instanceof Cell.CellGroup)
		{
			Cell.CellGroup group = (Cell.CellGroup)nodeInfo;
			Set groupNames = new TreeSet();
			for(Iterator it = group.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				groupNames.add(cell.getProtoName());
			}
			String groupName = null;
			for(Iterator it = groupNames.iterator(); it.hasNext(); )
			{
				String oneName = (String)it.next();
				if (groupName == null) groupName = oneName; else
					groupName += "," + oneName;
			}
			return groupName;
		}
		return nodeInfo.toString();
	}

	// if need custom image to show tree
	/* XXX not used, use MyRenderer instead
	public void addTreeBranchImage(ImageIcon icon)
	{
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(icon);
		setCellRenderer(renderer);
	}
	*/

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
					iconLibrary = new ImageIcon(getClass().getResource("IconLibrary.gif"));
				setIcon(iconLibrary);
			}
			if (nodeInfo instanceof Cell)
			{
				Cell cell = (Cell)nodeInfo;
				if (cell.getView() == View.ICON)
				{
					if (iconViewIcon == null)
						iconViewIcon = new ImageIcon(getClass().getResource("IconViewIcon.gif"));
					if (iconViewOldIcon == null)
						iconViewOldIcon = new ImageIcon(getClass().getResource("IconViewOldIcon.gif"));
					if (cell.getNewestVersion() == cell) setIcon(iconViewIcon); else
						setIcon(iconViewOldIcon);
				} else if (cell.getView() == View.LAYOUT)
				{
					if (iconViewLayout == null)
						iconViewLayout = new ImageIcon(getClass().getResource("IconViewLayout.gif"));
					if (iconViewOldLayout == null)
						iconViewOldLayout = new ImageIcon(getClass().getResource("IconViewOldLayout.gif"));
					if (cell.getNewestVersion() == cell) setIcon(iconViewLayout); else
						setIcon(iconViewOldLayout);
				} else if (cell.getView() == View.SCHEMATIC)
				{
					if (iconViewSchematics == null)
						iconViewSchematics = new ImageIcon(getClass().getResource("IconViewSchematics.gif"));
					if (iconViewOldSchematics == null)
						iconViewOldSchematics = new ImageIcon(getClass().getResource("IconViewOldSchematics.gif"));
					if (cell.getNewestVersion() == cell) setIcon(iconViewSchematics); else
						setIcon(iconViewOldSchematics);
				} else if (cell.getView().isTextView())
				{
					if (iconViewText == null)
						iconViewText = new ImageIcon(getClass().getResource("IconViewText.gif"));
					if (iconViewOldText == null)
						iconViewOldText = new ImageIcon(getClass().getResource("IconViewOldText.gif"));
					if (cell.getNewestVersion() == cell) setIcon(iconViewText); else
						setIcon(iconViewOldText);
				} else
				{
					if (iconViewMisc == null)
						iconViewMisc = new ImageIcon(getClass().getResource("IconViewMisc.gif"));
					if (iconViewOldMisc == null)
						iconViewOldMisc = new ImageIcon(getClass().getResource("IconViewOldMisc.gif"));
					if (cell.getNewestVersion() == cell) setIcon(iconViewMisc); else
						setIcon(iconViewOldMisc);
				}
			}
			if (nodeInfo instanceof Cell.CellGroup)
			{
				if (iconGroup == null)
					iconGroup = new ImageIcon(getClass().getResource("IconGroup.gif"));
				setIcon(iconGroup);
			}
			if (nodeInfo instanceof String)
			{
				String theString = (String)nodeInfo;
				if (theString.equalsIgnoreCase("jobs"))
				{
					if (iconJobs == null)
						iconJobs = new ImageIcon(getClass().getResource("IconJobs.gif"));
					setIcon(iconJobs);
				} else if (theString.equalsIgnoreCase("libraries"))
				{
					if (iconLibraries == null)
						iconLibraries = new ImageIcon(getClass().getResource("IconLibraries.gif"));
					setIcon(iconLibraries);
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


	static class TreeHandler implements MouseListener, MouseMotionListener
	{
		private ExplorerTree tree;
		private EditWindow wnd;
		private Object currentSelectedObject;
		private Cell originalCell;
		private boolean draggingCell;
		private MouseEvent currentMouseEvent;
		private TreePath currentPath;
		private TreePath originalPath;

		TreeHandler(ExplorerTree tree, EditWindow wnd) { this.tree = tree;   this.wnd = wnd; }

		public void mouseClicked(MouseEvent e) {}

		public void mouseEntered(MouseEvent e) {}

		public void mouseExited(MouseEvent e) {}

		public void mouseMoved(MouseEvent e) {}

		public void mousePressed(MouseEvent e)
		{
			draggingCell = false;
			cacheEvent(e);

			// popup menu event (right click)
			if (e.isPopupTrigger())
			{
				doContextMenu();
				return;
			}

			// double click
			if (e.getClickCount() == 2)
			{
				if (currentSelectedObject instanceof Cell)
				{
					Cell cell = (Cell)currentSelectedObject;
					wnd.setCell(cell, VarContext.globalContext);
					return;
				}
				if (currentSelectedObject instanceof Library || currentSelectedObject instanceof Cell.CellGroup ||
					currentSelectedObject instanceof String)
				{
					if (tree.isExpanded(currentPath)) tree.collapsePath(currentPath); else
						tree.expandPath(currentPath);
					return;
				}

				if (currentSelectedObject instanceof Job)
				{
					Job job = (Job)currentSelectedObject;
					System.out.println(job.getInfo());
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
			cacheEvent(e);
			if (e.isPopupTrigger()) doContextMenu();
		}

		public void mouseDragged(MouseEvent e)
		{
			if (!draggingCell) return;
			cacheEvent(e);
			tree.clearSelection();
			tree.addSelectionPath(originalPath);
			tree.addSelectionPath(currentPath);
			tree.updateUI();

//			tree.treeDidChange();
//			EditWindow.redrawAll();
		}

		private void cacheEvent(MouseEvent e)
		{
			currentPath = tree.getPathForLocation(e.getX(), e.getY());
			if (currentPath == null) return;
			tree.setSelectionPath(currentPath);
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)currentPath.getLastPathComponent();
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

				menuItem = new JMenuItem("Create New Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellAction(); } });

				menuItem = new JMenuItem("Create New Version of Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { newCellVersionAction(); } });

				menuItem = new JMenuItem("Duplicate Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { duplicateCellAction(); } });

				menu.addSeparator();

				menuItem = new JMenuItem("Delete Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteCellAction(); } });

				menuItem = new JMenuItem("Rename Cell");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { renameCellAction(); } });

				menu.addSeparator();

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

				menuItem = new JMenuItem("Delete Library");
				menu.add(menuItem);
				menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteLibraryAction(); } });

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

				menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
				return;
			}
			if (currentSelectedObject instanceof String)
			{
				String msg = (String)currentSelectedObject;
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

//					menuItem = new JMenuItem("Show Cells by Hierarchy");
//					menu.add(menuItem);
//					menuItem.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { showByHierarchyAction(); } });

					menu.show((Component)currentMouseEvent.getSource(), currentMouseEvent.getX(), currentMouseEvent.getY());
					return;
				}
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
			Library.setCurrent(lib);
			explorerTreeChanged();
			EditWindow.redrawAll();
		}

		private void renameLibraryAction()
		{
			Library lib = (Library)currentSelectedObject;
			String response = JOptionPane.showInputDialog(tree, "New name for library:", lib.getLibName());
			if (response == null) return;
			System.out.println("Want to rename library " + lib.getLibName() + " to " + response + " BUT CAN'T YET");
		}

		private void deleteLibraryAction()
		{
			Library lib = (Library)currentSelectedObject;
			int response = JOptionPane.showConfirmDialog(tree, "Are you sure you want to delete library " + lib.getLibName() + "?");
			if (response != JOptionPane.YES_OPTION) return;
			System.out.println("Library " + lib.getLibName() + " deleted");
			lib.kill();
			ExplorerTree.explorerTreeChanged();
		}

		private void editCellAction(boolean newWindow)
		{
			Cell cell = (Cell)currentSelectedObject;
			if (newWindow)
			{
				WindowFrame wf = WindowFrame.createEditWindow(cell);
			} else
			{
				wnd.setCell(cell, VarContext.globalContext);
			}
		}

		private void newCellAction()
		{
			JFrame jf = TopLevel.getCurrentJFrame();
			NewCell dialog = new NewCell(jf, true);
			dialog.show();
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
				cell.getProtoName() + "NEW");
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
			String response = JOptionPane.showInputDialog(tree, "New name for cell " + cell.describe(), cell.getProtoName());
			if (response == null) return;
			System.out.println("Want to rename cell " + cell.getProtoName() + " to " + response + " BUT CAN'T YET");
		}

		private void reViewCellAction(ActionEvent e)
		{
			JMenuItem menuItem = (JMenuItem)e.getSource();
			String viewName = menuItem.getText();
			View newView = View.findView(viewName);
			if (newView != null)
			{
				Cell cell = (Cell)currentSelectedObject;
				CircuitChanges.changeCellView(cell, newView);
			}
		}

		private void showAlphabeticallyAction()
		{
			howToShow = SHOWALPHABETICALLY;
			explorerTreeChanged();
			EditWindow.redrawAll();
		}

		private void showByGroupAction()
		{
			howToShow = SHOWBYCELLGROUP;
			explorerTreeChanged();
			EditWindow.redrawAll();
		}

		private void showByHierarchyAction()
		{
			howToShow = SHOWBYHIERARCHY;
			explorerTreeChanged();
			EditWindow.redrawAll();
		}

	}
}
