/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TreeView.java
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

import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultMutableTreeNode;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.Job;

/**
 * Class to display a cell explorer tree-view of the database.
 */
public class TreeView extends JTree 
{
	private TreeHandler handler = null;

	private TreeView(TreeNode str)
	{
		super(str);

		// single selection as default
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// do not show top-level
		setRootVisible(false);
		setShowsRootHandles(true);

		// show one level of indentation
		//	collapseRow(1);

		// enable tool tips - we'll use these to display useful info
		ToolTipManager.sharedInstance().registerComponent(this);

		// register our own extended renderer for custom icons and tooltips
		setCellRenderer(new MyRenderer());
	}

	public static TreeView CreateTreeView(TreeNode str, EditWindow wnd)
	{
		final TreeView tree = new TreeView(str);
		tree.handler = new TreeHandler();
		tree.handler.setTreeView(tree);
		tree.handler.setTreeWindow(wnd);
		tree.addTreeSelectionListener(tree.handler);
		tree.addMouseListener(tree.handler);
		tree.addMouseMotionListener(tree.handler);
		return tree;
	}

	public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf,
		int row, boolean hasFocus)
	{
		Object nodeInfo = ((DefaultMutableTreeNode)value).getUserObject();
		if (nodeInfo instanceof Cell)
		{
			Cell cell = (Cell)nodeInfo;
			return cell.noLibDescribe();
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
			super.getTreeCellRendererComponent(tree, value, sel,
			expanded, leaf, row, hasFocus);
			// setIcon(icon)
			//setToolTipText(value.toString());
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
			Object nodeInfo = node.getUserObject();
			if (nodeInfo instanceof Job)
			{
				Job j = (Job)nodeInfo;
				//setToolTipText(j.getToolTip());
				//System.out.println("set tool tip to "+j.getToolTip());
			}

			return this;
		}
	}
}
