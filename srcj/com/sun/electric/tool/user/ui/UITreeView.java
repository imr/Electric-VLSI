/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UITreeView.java
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

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.tree.DefaultMutableTreeNode;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.user.ui.UIEdit;

/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class UITreeView extends JTree 
{
	private UITreeHandler handler = null;

	private UITreeView(TreeNode str)
	{
		super(str);

		// single selection as default
		getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

		// show handle as default
		setShowsRootHandles(true);

		// show one level of indentation
		collapseRow(1);
	}

	public static UITreeView CreateTreeView(TreeNode str, UIEdit wnd)
	{
		final UITreeView tree = new UITreeView(str);
		tree.handler = new UITreeHandler();
		tree.handler.setTreeView(tree);
		tree.handler.setTreeWindow(wnd);
		tree.addTreeSelectionListener(tree.handler);
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
	public void addTreeBranchImage(ImageIcon icon)
	{
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
		renderer.setLeafIcon(icon);
		setCellRenderer(renderer);
	}
}
