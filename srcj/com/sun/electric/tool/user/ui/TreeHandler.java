/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TreeHandler.java
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
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;

import javax.swing.JPopupMenu;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;

/**
 * @author wc147374
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TreeHandler implements TreeSelectionListener, MouseListener, MouseMotionListener
{
	private TreeView tree;
	private EditWindow wnd;
    
    /** current does not do anything because not registered as tree
     * selection listener, using MouseListener instead */
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
				wnd.setCell(cell, VarContext.globalContext);
			}
		} else
		{
			// clicked on branch node
		}
		
	}
	
	public TreeView getTreeView() { return tree; }
	
	public void setTreeView(TreeView tree) { this.tree = tree; }
	
	public void setTreeWindow(EditWindow wnd) { this.wnd = wnd; }
    
    public void mouseClicked(MouseEvent e) {

    }
    
    public void mouseEntered(MouseEvent e) {
    }
    
    public void mouseExited(MouseEvent e) {
    }
    
    public void mousePressed(MouseEvent e) {
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path == null) return;
        tree.setSelectionPath(path);
        Object obj = path.getLastPathComponent();
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
        Object nodeInfo = node.getUserObject();

        // popup menu event (right click)
        if (e.isPopupTrigger()) {
            Object source = e.getSource();
            if (!(source instanceof Component)) return;
            // show Job menu if user clicked on a Job
            if (nodeInfo instanceof Job) {
                Job job = (Job)nodeInfo;
                JPopupMenu popup = job.getPopupStatus();
                popup.show((Component)source, e.getX(), e.getY());
                return;
            }
            if (nodeInfo instanceof Cell) {
                Cell cell = (Cell)nodeInfo;
                // TODO: get popup menu for Cell
                return;
            }
        }
        // regular click
        if (nodeInfo instanceof Cell) {
            Cell cell = (Cell)nodeInfo;
            wnd.setCell(cell, VarContext.globalContext);
            return;
        }
        if (nodeInfo instanceof Job) {
            Job job = (Job)nodeInfo;
            return;
        }
    }
    
    public void mouseReleased(MouseEvent e) {
    }
    
    public void mouseDragged(MouseEvent e) {
    }
    
    public void mouseMoved(MouseEvent e) {
    }
    
}
