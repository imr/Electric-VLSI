/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UITreeHandler.java
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

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class UITreeHandler implements TreeSelectionListener 
{
	private UITreeView tree;
	private UIEdit wnd;
	
	public void valueChanged(TreeSelectionEvent arg0) 
	{
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)tree.getLastSelectedPathComponent();
		if (node == null) return;		
		
		Object nodeInfo = node.getUserObject();
		
		if (node.isLeaf())
		{
			// clicked on leaf node
			if (nodeInfo instanceof Cell)
			{
				Cell cell = (Cell)nodeInfo;
				wnd.setCell(cell);
			}
		} else
		{
			// clicked on branch node
		}
		
	}
	
	public UITreeView getTreeView() { return tree; }
	
	public void setTreeView(UITreeView tree) { this.tree = tree; }
	
	public void setTreeWindow(UIEdit wnd) { this.wnd = wnd; }
}
